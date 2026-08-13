package AttizosBackend.service;

import AttizosBackend.model.DetalleCombo;
import AttizosBackend.model.Producto;
import AttizosBackend.model.Promocion;
import AttizosBackend.websocket.SyncSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Service
public class PromocionService {

    @Autowired
    private JdbcTemplate db;
    @Autowired
    private  SyncSocketHandler socketHandler;

    public java.util.List<Promocion> cargarPromocionesActivas() {
        String sql = "SELECT * FROM productos WHERE categoria = 'Promocion' AND estado = 'Activo'";

        return db.query(sql, (rs, rowNum) -> {
            Promocion promo = new Promocion();
            promo.setId(rs.getInt("id_producto"));
            promo.setNombre(rs.getString("nombre"));
            promo.setPrecio(rs.getDouble("precio"));
            promo.setImagenURL(rs.getString("imagen_base64"));
            promo.setEstado(rs.getString("estado"));

            if (rs.getDate("fecha_inicio") != null) promo.setFechaInicio(rs.getDate("fecha_inicio").toLocalDate());
            if (rs.getDate("fecha_fin") != null) promo.setFechaFin(rs.getDate("fecha_fin").toLocalDate());

            String sqlDetalle = "SELECT dc.cantidad, p.id_producto, p.nombre, p.precio FROM detalle_combo dc JOIN productos p ON dc.id_producto = p.id_producto WHERE dc.id_promocion = ?";
            java.util.List<AttizosBackend.model.DetalleCombo> detalles = db.query(sqlDetalle, (rsDet, rowNumDet) -> {
                AttizosBackend.model.DetalleCombo dc = new AttizosBackend.model.DetalleCombo();
                dc.setCantidad(rsDet.getInt("cantidad"));

                Producto prodFisico = new Producto();
                prodFisico.setId(rsDet.getInt("id_producto"));
                prodFisico.setNombre(rsDet.getString("nombre"));
                prodFisico.setPrecio(rsDet.getDouble("precio"));
                dc.setProducto(prodFisico);

                return dc;
            }, promo.getId());

            promo.setProductosCombo(detalles);
            return promo;
        });
    }

    public List<Map<String, Object>> obtenerDetallesDePromocion(int idPromocion) {
        return db.queryForList("SELECT id_producto, cantidad FROM detalle_combo WHERE id_promocion = ?", idPromocion);
    }

    @Transactional
    public Promocion guardarNuevaPromocion(Promocion promo) {
        String sqlProducto = "INSERT INTO productos (nombre, precio, categoria, stock_directo, imagen_base64, estado, fecha_inicio, fecha_fin) VALUES (?, ?, 'Promocion', 0, ?, 'Activo', ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        db.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sqlProducto, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, promo.getNombre());
            ps.setDouble(2, promo.getPrecio());
            ps.setString(3, promo.getImagenURL());
            ps.setObject(4, promo.getFechaInicio());
            ps.setObject(5, promo.getFechaFin());
            return ps;
        }, keyHolder);

        int idPromocionGenerado = ((Number) keyHolder.getKeys().get("id_producto")).intValue();
        promo.setId(idPromocionGenerado);

        if (!promo.getProductosCombo().isEmpty()) {
            String sqlDetalle = "INSERT INTO detalle_combo (id_promocion, id_producto, cantidad) VALUES (?, ?, ?)";
            db.batchUpdate(sqlDetalle, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    DetalleCombo detalle = promo.getProductosCombo().get(i);
                    ps.setInt(1, idPromocionGenerado);
                    ps.setInt(2, detalle.getProducto().getId());
                    ps.setInt(3, detalle.getCantidad());
                }

                @Override
                public int getBatchSize() {
                    return promo.getProductosCombo().size();
                }
            });
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                socketHandler.notificarAClientes("{\"evento\": \"SYNC_CATALOGO\"}");
            }
        });
        return promo;
    }
    @Transactional
    public int verificarYDesactivarPromociones() {
        String sql = "UPDATE productos SET estado = 'Inactivo' WHERE categoria = 'Promocion' AND estado = 'Activo' AND fecha_fin IS NOT NULL AND fecha_fin < CURRENT_DATE";
        int afectadas = db.update(sql);

        if (afectadas > 0) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    socketHandler.notificarAClientes("{\"evento\": \"SYNC_CATALOGO\"}");
                }
            });
        }
        return afectadas;
    }
}