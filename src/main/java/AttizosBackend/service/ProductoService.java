package AttizosBackend.service;

import AttizosBackend.model.Producto;
import AttizosBackend.model.Receta;
import AttizosBackend.websocket.SyncSocketHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;

@Service
public class ProductoService {
    @Autowired
    private JdbcTemplate db;
    @Autowired
    private SyncSocketHandler socketHandler;
    private ObjectMapper mapper = new ObjectMapper();

    @Transactional
    public Producto insertarProducto(Producto p){
        String sql = "INSERT INTO productos (nombre, precio, categoria, tipo_clase, stock_directo, tiene_receta, imagen_base64, atributos_extra, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, 'Activo')";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        db.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.getNombre());
            ps.setDouble(2, p.getPrecio());
            ps.setString(3, p.getCategoria());
            ps.setString(4, "Producto");
            ps.setInt(5, (int) p.getStock());
            ps.setBoolean(6, p.tieneReceta());
            ps.setString(7, p.getImagenURL());
            try {
                ps.setString(8, mapper.writeValueAsString(p.getAtributosDinamicos()));
            } catch (Exception e) {
                ps.setString(8, "{}");
            }
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null) {
            p.setId(((Number) keyHolder.getKeys().get("id_producto")).intValue());
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                socketHandler.notificarAClientes("{\"evento\": \"SYNC_CATALOGO\"}");
            }
        });
        return p;
    }
    public java.util.List<Producto> obtenerMenuCompleto() {
        String sql = "SELECT * FROM productos WHERE estado = 'Activo' ORDER BY id_producto";

        return db.query(sql, (rs, rowNum) -> {
            Producto p = new Producto();
            p.setId(rs.getInt("id_producto"));
            p.setNombre(rs.getString("nombre"));
            p.setPrecio(rs.getDouble("precio"));
            p.setCategoria(rs.getString("categoria"));
            p.setStock(rs.getDouble("stock_directo"));
            p.setTieneReceta(rs.getBoolean("tiene_receta"));
            p.setImagenURL(rs.getString("imagen_base64"));
            p.setEstado(rs.getString("estado"));

            String jsonAtributos = rs.getString("atributos_extra");
            if (jsonAtributos != null && !jsonAtributos.trim().isEmpty()) {
                try {
                    p.setAtributosDinamicos(mapper.readValue(jsonAtributos, new TypeReference<Map<String, String>>() {}));
                } catch (Exception e) {}
            }

            if (p.tieneReceta()) {
                java.util.List<Map<String, Object>> detalles = db.queryForList("SELECT codigo_insumo, cantidad_necesaria FROM recetas_detalle WHERE id_producto = ?", p.getId());
                Receta recetaObj = new Receta();
                for (Map<String, Object> det : detalles) {
                    recetaObj.getIngredientes().put((String) det.get("codigo_insumo"), ((Number) det.get("cantidad_necesaria")).doubleValue());
                }
                p.setReceta(recetaObj);
            }
            return p;
        });
    }

    @Transactional
    public boolean actualizarProducto(Producto p) {
        String sql = "UPDATE productos SET nombre = ?, precio = ?, categoria = ?, stock_directo = ?, imagen_base64 = ?, atributos_extra = ?::jsonb WHERE id_producto = ?";
        try {
            String jsonAtributos = mapper.writeValueAsString(p.getAtributosDinamicos());
            boolean exito = db.update(sql, p.getNombre(), p.getPrecio(), p.getCategoria(), (int) p.getStock(), p.getImagenURL(), jsonAtributos, p.getId()) > 0;
            if(exito){
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        socketHandler.notificarAClientes("{\"evento\": \"SYNC_CATALOGO\"}");
                    }
                });
            }
            return exito;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public boolean eliminarProducto(int idProducto) {
        boolean exito = db.update("UPDATE productos SET estado = 'Inactivo' WHERE id_producto = ?", idProducto) > 0;
        if (exito){
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    socketHandler.notificarAClientes("{\"evento\": \"SYNC_CATALOGO\"}");
                }
            });
        }
        return exito;
    }

    @Transactional
    public boolean actualizarImagenProducto(int idProducto, String nuevaURL) {
        boolean exito =  db.update("UPDATE productos SET imagen_base64 = ? WHERE id_producto = ?", nuevaURL, idProducto) > 0;
        if(exito){
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    socketHandler.notificarAClientes("{\"evento\": \"SYNC_CATALOGO\"}");
                }
            });
        }
        return exito;
    }

}
