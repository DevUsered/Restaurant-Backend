package AttizosBackend.service;

import AttizosBackend.model.Insumo;
import AttizosBackend.websocket.SyncSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class InsumoService {

    @Autowired
    private JdbcTemplate db;
    @Autowired
    private SyncSocketHandler socketHandler;

    @Transactional
    public java.util.List<Insumo> obtenerInventarioActivo() {
        String sql = "SELECT c.codigo, c.nombre, c.categoria, c.unidad_medida, c.stock_minimo, c.stock_maximo, " +
                     "COALESCE(SUM(l.stock_actual), 0) AS stock_total, " +
                     "MIN(l.fecha_vencimiento) AS proximo_vencimiento " +
                     "FROM insumos_catalogo c " +
                     "LEFT JOIN insumos_lotes l ON c.codigo = l.codigo_insumo AND l.estado = 'Activo' " +
                     "WHERE c.estado = 'Activo' " +
                     "GROUP BY c.codigo, c.nombre, c.categoria, c.unidad_medida, c.stock_minimo, c.stock_maximo";

        return db.query(sql, (rs, rowNum) -> {
            Insumo i = new Insumo();
            i.setCodigo(rs.getString("codigo"));
            i.setNombre(rs.getString("nombre"));
            i.setCategoria(rs.getString("categoria"));
            i.setUnidad(rs.getString("unidad_medida"));
           i.setStockMinimo(rs.getDouble("stock_minimo"));
            i.setStockMaximo(rs.getDouble("stock_maximo"));
            
            // Llenamos los campos @Transient
            i.setStockActual(rs.getDouble("stock_total"));
            
            java.sql.Date dbDate = rs.getDate("proximo_vencimiento");
            if (dbDate != null) {
                i.setFechaVencimiento(dbDate.toLocalDate());
            } else {
                i.setFechaVencimiento(java.time.LocalDate.now().plusYears(6));
            }
            
            i.setEstado("Activo");
            return i;
        });
    }


    public String generarSiguienteCodigo() {
        String sql = "SELECT MAX(CAST(SUBSTRING(codigo, 5) AS INTEGER)) FROM insumos_catalogo WHERE codigo LIKE 'INS-%'";
        Integer maxNumero = db.queryForObject(sql, Integer.class);
        return (maxNumero != null) ? String.format("INS-%03d", maxNumero + 1) : "INS-001";
    }

    // @Transactional hace el commit automático, y si hay error, hace el rollback solo.
    @Transactional
    public boolean insertarInsumoNuevo(Insumo insumo, double costoInicial) {
        String sqlCatalogo = "INSERT INTO insumos_catalogo (codigo, nombre, categoria, unidad_medida, stock_minimo, stock_maximo, estado) VALUES (?, ?, ?, ?, ?, ?, 'Activo')";
        db.update(sqlCatalogo, insumo.getCodigo(), insumo.getNombre(), insumo.getCategoria(), insumo.getUnidad(), insumo.getStockMinimo(), insumo.getStockMaximo());

        String sqlLote = "INSERT INTO insumos_lotes (codigo_insumo, stock_actual, fecha_ingreso, fecha_vencimiento, costo_compra, estado) VALUES (?, ?, CURRENT_DATE, ?, ?, 'Activo')";
        LocalDate vencimiento = (insumo.getFechaVencimiento() != null) ? insumo.getFechaVencimiento() : LocalDate.now().plusYears(10);
        db.update(sqlLote, insumo.getCodigo(), insumo.getStockActual(), vencimiento, costoInicial);

        socketHandler.notificarAClientes("{\"evento\": \"SYNC_INVENTARIO\"}");
        
        return true;
    }

    @Transactional
    public boolean registrarNuevaCompraLote(String codigoInsumo, double cantidadComprada, double costo, LocalDate vencimiento) {
        String sqlLote = "INSERT INTO insumos_lotes (codigo_insumo, stock_actual, fecha_ingreso, fecha_vencimiento, costo_compra, estado) VALUES (?, ?, CURRENT_DATE, ?, ?, 'Activo')";
        boolean exito = db.update(sqlLote, codigoInsumo, cantidadComprada, vencimiento, costo) > 0;
        if (exito) socketHandler.notificarAClientes("{\"evento\": \"SYNC_INVENTARIO\"}");
        return exito;
    }

    @Transactional
    public boolean darDeBajaInsumo(String codigo) {
        db.update("UPDATE insumos_lotes SET estado = 'Inactivo' WHERE codigo_insumo = ?", codigo);
        String sqlCatalogo = "UPDATE insumos_catalogo SET estado = 'Inactivo', nombre = CONCAT(nombre, ' [BAJA-', codigo, ']') WHERE codigo = ?";
        db.update(sqlCatalogo, codigo);

        socketHandler.notificarAClientes("{\"evento\": \"SYNC_INVENTARIO\"}");
        return true;
    }

    @Transactional
    public double darDeBajaLotesVencidos(String codigoInsumo) {
        String sqlSelect = "SELECT COALESCE(SUM(stock_actual), 0) FROM insumos_lotes WHERE codigo_insumo = ? AND fecha_vencimiento < CURRENT_DATE AND estado = 'Activo'";
        Double totalDescontado = db.queryForObject(sqlSelect, Double.class, codigoInsumo);

        String sqlUpdate = "UPDATE insumos_lotes SET estado = 'Inactivo', stock_actual = 0 WHERE codigo_insumo = ? AND fecha_vencimiento < CURRENT_DATE AND estado = 'Activo'";
        db.update(sqlUpdate, codigoInsumo);

        return (totalDescontado != null) ? totalDescontado : 0.0;
    }
    @Transactional
    public boolean descontarStockFEFO(String codigoInsumo, double cantidadRequerida) {
        String sqlSelect = "SELECT id_lote, stock_actual FROM insumos_lotes " +
                           "WHERE codigo_insumo = ? AND estado = 'Activo' AND stock_actual > 0 " +
                           "AND fecha_vencimiento >= CURRENT_DATE " +
                           "ORDER BY fecha_vencimiento ASC, id_lote ASC";

        String sqlUpdateLote = "UPDATE insumos_lotes SET stock_actual = ?, estado = ? WHERE id_lote = ?";

        java.util.List<java.util.Map<String, Object>> lotes = db.queryForList(sqlSelect, codigoInsumo);

        double cantidadFaltante = cantidadRequerida;

        for (java.util.Map<String, Object> lote : lotes) {
            if (cantidadFaltante <= 0) break;

            int idLote = ((Number) lote.get("id_lote")).intValue();
            double stockLote = ((Number) lote.get("stock_actual")).doubleValue();

            double nuevoStockLote;
            String nuevoEstado = "Activo";

            if (stockLote <= cantidadFaltante) {
                cantidadFaltante -= stockLote;
                nuevoStockLote = 0;
                nuevoEstado = "Inactivo";
            } else {
                nuevoStockLote = stockLote - cantidadFaltante;
                cantidadFaltante = 0.0;
            }

            db.update(sqlUpdateLote, nuevoStockLote, nuevoEstado, idLote);
        }

        if (cantidadFaltante > 0) {
            System.err.println("❌ Stock insuficiente en almacén para: " + codigoInsumo);
            throw new RuntimeException("Stock insuficiente para el insumo: " + codigoInsumo);
        }

        return true;
    }
}