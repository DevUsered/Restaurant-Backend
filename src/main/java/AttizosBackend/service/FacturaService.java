package AttizosBackend.service;

import AttizosBackend.model.ItemCarritoDTO;
import AttizosBackend.model.VentaRequest;
import AttizosBackend.websocket.SyncSocketHandler;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FacturaService {
    @Autowired
    private JdbcTemplate db;

    @Autowired
    private SyncSocketHandler socketHandler;


    @Transactional
    public Map<String, Integer> registrarVenta(VentaRequest request) {
        String sqlDiario = "SELECT COALESCE(MAX(numero_ticket), 0) + 1 FROM facturas WHERE CAST(fecha_hora AS DATE) = CURRENT_DATE";
        int numeroTicketDiario = db.queryForObject(sqlDiario, Integer.class);

        String sqlFactura = "INSERT INTO facturas (nombre_cliente, total, estado, numero_ticket, fecha_hora, metodo_pago) VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        db.update(con -> {
            PreparedStatement ps = con.prepareStatement(sqlFactura, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, request.getNombreCliente());
            ps.setDouble(2, request.getTotal());
            ps.setString(3, request.getEstado());
            ps.setInt(4, numeroTicketDiario);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(6, request.getMetodoPago() != null ? request.getMetodoPago() : "Efectivo");
            return ps;
        }, keyHolder);

        int numeroFactura = ((Number) keyHolder.getKeys().get("numero_factura")).intValue();
        boolean requiereCocina = false;

        for (ItemCarritoDTO item : request.getItems()) {
            double subtotal = item.getPrecio() * item.getCantidad();

            db.update("INSERT INTO facturas_detalle (numero_factura, id_producto, cantidad, subtotal) VALUES (?, ?, ?, ?)",
                    numeroFactura, item.getIdProducto(), item.getCantidad(), subtotal);

            if (!item.isTieneReceta()) {
                db.update("UPDATE productos SET stock_directo = stock_directo - ? WHERE id_producto = ?",
                        item.getCantidad(), item.getIdProducto());
            } else {
                requiereCocina = true;

                List<Map<String, Object>> ingredientesReceta = db.queryForList(
                        "SELECT codigo_insumo, cantidad_necesaria FROM recetas_detalle WHERE id_producto = ?", item.getIdProducto());

                for (Map<String, Object> ing : ingredientesReceta) {
                    String codInsumo = (String) ing.get("codigo_insumo");
                    double cantUnitaria = ((Number) ing.get("cantidad_necesaria")).doubleValue();
                    double totalNecesario = cantUnitaria * item.getCantidad();
                    double cantidadFaltante = totalNecesario;

                    String sqlSelectLotes = "SELECT id_lote, stock_actual FROM insumos_lotes " +
                            "WHERE codigo_insumo = ? AND estado = 'Activo' AND stock_actual > 0 " +
                            "ORDER BY fecha_vencimiento ASC, id_lote ASC FOR UPDATE";

                    List<Map<String, Object>> lotesDisponibles = db.queryForList(sqlSelectLotes, codInsumo);

                    for (Map<String, Object> lote : lotesDisponibles) {
                        if (cantidadFaltante <= 0) break;

                        int idLote = (Integer) lote.get("id_lote");
                        double stockLote = ((Number) lote.get("stock_actual")).doubleValue();

                        double descuentoDeEsteLote;
                        double nuevoStockLote;
                        String nuevoEstado = "Activo";

                        if (stockLote <= cantidadFaltante) {
                            descuentoDeEsteLote = stockLote;
                            cantidadFaltante -= stockLote;
                            nuevoStockLote = 0.0;
                            nuevoEstado = "Inactivo";
                        } else {
                            descuentoDeEsteLote = cantidadFaltante;
                            nuevoStockLote = stockLote - cantidadFaltante;
                            cantidadFaltante = 0.0;
                        }

                        db.update("UPDATE insumos_lotes SET stock_actual = ?, estado = ? WHERE id_lote = ?",
                                nuevoStockLote, nuevoEstado, idLote);

                        db.update("INSERT INTO lotes_consumidos_venta (numero_factura, id_lote, cantidad_descontada) VALUES (?, ?, ?) " +
                                        "ON CONFLICT (numero_factura, id_lote) " +
                                        "DO UPDATE SET cantidad_descontada = lotes_consumidos_venta.cantidad_descontada + EXCLUDED.cantidad_descontada",
                                numeroFactura, idLote, descuentoDeEsteLote);
                        db.update("INSERT INTO kardex_inventario (codigo_insumo, tipo_movimiento, cantidad, motivo, id_lote) VALUES (?, 'EGRESO_VENTA', ?, ?, ?)",
                                codInsumo, descuentoDeEsteLote, "Venta Ticket #" + numeroTicketDiario, idLote);
                    }
                    if (cantidadFaltante > 0) {
                        throw new RuntimeException("Stock insuficiente en lotes para el insumo código: " + codInsumo);
                    }
                }
            }
        }

        if (requiereCocina) {
            db.update("INSERT INTO cola_cocina (id_pedido, numero_ticket, estado) VALUES (?, ?, 'Pendiente')",
                    numeroFactura, numeroTicketDiario);
            socketHandler.notificarAClientes("{\"evento\": \"SYNC_PEDIDOS\"}");
        }

        Map<String, Integer> respuesta = new HashMap<>();
        respuesta.put("numeroFactura", numeroFactura);
        respuesta.put("numeroTicket", numeroTicketDiario);

        final boolean notificarCocina = requiereCocina;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (notificarCocina) {
                    socketHandler.notificarAClientes("{\"evento\": \"SYNC_PEDIDOS\"}");
                }
                socketHandler.notificarAClientes("{\"evento\": \"SYNC_INVENTARIO\"}");
                socketHandler.notificarAClientes("{\"evento\": \"SYNC_REPORTES\"}");
            }
        });
        return respuesta;
    }
    @Transactional
    public boolean anularVenta(int numeroFactura) {
        db.update("UPDATE facturas SET estado = 'Anulada' WHERE numero_factura = ?", numeroFactura);
        db.update("DELETE FROM cola_cocina WHERE id_pedido = ?", numeroFactura);

        String sqlSelectVitrina = "SELECT fd.id_producto, fd.cantidad FROM facturas_detalle fd " +
                "INNER JOIN productos p ON fd.id_producto = p.id_producto " +
                "WHERE fd.numero_factura = ? AND p.tiene_receta = false";

        List<Map<String, Object>> productosVitrina = db.queryForList(sqlSelectVitrina, numeroFactura);
        for (Map<String, Object> pv : productosVitrina) {
            db.update("UPDATE productos SET stock_directo = stock_directo + ? WHERE id_producto = ?",
                    (Integer) pv.get("cantidad"), (Integer) pv.get("id_producto"));
        }

        String sqlSelectLotesUsados = "SELECT id_lote, cantidad_descontada FROM lotes_consumidos_venta WHERE numero_factura = ?";
        List<Map<String, Object>> lotesUsados = db.queryForList(sqlSelectLotesUsados, numeroFactura);

        for (Map<String, Object> lu : lotesUsados) {
            db.update("UPDATE insumos_lotes SET stock_actual = stock_actual + ?, estado = 'Activo' WHERE id_lote = ?",
                    ((Number) lu.get("cantidad_descontada")).doubleValue(), (Integer) lu.get("id_lote"));
            db.update("INSERT INTO kardex_inventario (codigo_insumo, tipo_movimiento, cantidad, motivo, id_lote) " +
                            "SELECT codigo_insumo, 'REINGRESO_ANULACION', ?, ?, id_lote FROM insumos_lotes WHERE id_lote = ?",
                    ((Number) lu.get("cantidad_descontada")).doubleValue(), "Anulación Factura #" + numeroFactura, (Integer) lu.get("id_lote"));

        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                socketHandler.notificarAClientes("{\"evento\": \"SYNC_INVENTARIO\"}");
                socketHandler.notificarAClientes("{\"evento\": \"SYNC_PEDIDOS\"}");
                socketHandler.notificarAClientes("{\"evento\": \"SYNC_REPORTES\"}");
            }
        });
        return true;
    }
    public Map<String, Object> obtenerFacturaConDetalles(int numeroFactura) {
        String sqlFac = "SELECT nombre_cliente, total, fecha_hora, estado, numero_ticket FROM facturas WHERE numero_factura = ?";
        Map<String, Object> factura = db.queryForMap(sqlFac, numeroFactura);

        String sqlDet = "SELECT fd.cantidad, fd.subtotal, p.id_producto, p.nombre, p.precio " +
                "FROM facturas_detalle fd " +
                "INNER JOIN productos p ON fd.id_producto = p.id_producto " +
                "WHERE fd.numero_factura = ?";
        List<Map<String, Object>> detalles = db.queryForList(sqlDet, numeroFactura);
        factura.put("detalles", detalles);

        return factura;
    }
}
