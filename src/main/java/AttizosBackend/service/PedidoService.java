package AttizosBackend.service;

import AttizosBackend.websocket.SyncSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PedidoService {
    @Autowired
    private JdbcTemplate db;
    @Autowired
    private FacturaService facturaService;
    @Autowired
    private SyncSocketHandler socketHandler;

    public List<Map<String, Object>> obtenerPedidosPendientes() {
        String sql = "SELECT cc.id_pedido, cc.numero_ticket, cc.estado, f.nombre_cliente " +
                "FROM cola_cocina cc " +
                "INNER JOIN facturas f ON cc.id_pedido = f.numero_factura " +
                "WHERE cc.estado = 'Pendiente' ORDER BY cc.id_pedido ASC";

        List<Map<String, Object>> filas = db.queryForList(sql);
        List<Map<String, Object>> resultados = new ArrayList<>();

        for (Map<String, Object> fila : filas) {
            Map<String, Object> pedido = new HashMap<>();
            pedido.put("idPedido", fila.get("id_pedido"));
            pedido.put("numeroTicket", fila.get("numero_ticket"));
            pedido.put("estado", fila.get("estado"));
            pedido.put("cliente", fila.get("nombre_cliente"));
            resultados.add(pedido);
        }
        return resultados;
    }
    public List<String> obtenerDetallesParaCocina(int idPedido) {
        String sql = "SELECT fd.cantidad, p.nombre FROM facturas_detalle fd " +
                "INNER JOIN productos p ON fd.id_producto = p.id_producto " +
                "WHERE fd.numero_factura = ? AND p.tiene_receta = true";

        return db.query(sql, (rs, rowNum) ->
                rs.getInt("cantidad") + "x  " + rs.getString("nombre"), idPedido
        );
    }
    @Transactional
    public boolean eliminarPedidoDespachado(int idPedido) {
        db.update("UPDATE facturas SET estado = 'Finalizada' WHERE numero_factura = ?", idPedido);
        boolean exito =  db.update("DELETE FROM cola_cocina WHERE id_pedido = ?", idPedido) > 0;
        if(exito) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    socketHandler.notificarAClientes("{\"evento\": \"SYNC_PEDIDOS\"}");
                    socketHandler.notificarAClientes("{\"evento\": \"SYNC_REPORTES\"}");
                }
            });
        }

        return exito;
    }
    @Transactional
    public boolean cancelarPedidoYAnularVenta(int idPedido) {
        return facturaService.anularVenta(idPedido);
    }
}
