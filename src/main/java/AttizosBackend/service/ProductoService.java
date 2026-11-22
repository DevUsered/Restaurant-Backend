package AttizosBackend.service;

import AttizosBackend.model.Producto;
import AttizosBackend.model.Receta;
import AttizosBackend.websocket.SyncSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductoService {
    @Autowired
    private JdbcTemplate db;

    @Autowired
    private SyncSocketHandler socketHandler;

    private final ObjectMapper mapper = new ObjectMapper();

    @Transactional
    public Producto insertarProducto(Producto p) {
        String sql = "INSERT INTO productos (nombre, precio, categoria, tipo_clase, stock_directo, tiene_receta, imagen_base64, atributos_extra, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, 'Activo')";

        String jsonAtributos = serializarAtributos(p);
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
            ps.setString(8, jsonAtributos);
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null && keyHolder.getKeys().get("id_producto") != null) {
            p.setId(((Number) keyHolder.getKeys().get("id_producto")).intValue());
        }

        notificarSyncCatalogo();
        return p;
    }

    public List<Producto> obtenerMenuCompleto() {
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
                    Map<String, String> attrs = mapper.readValue(
                            jsonAtributos,
                            mapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class)
                    );
                    if (attrs != null) {
                        p.setAtributosDinamicos(attrs);
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Error leyendo atributos_extra del producto " + p.getId() + ": " + e.getMessage());
                }
            }

            if (p.tieneReceta()) {
                List<Map<String, Object>> detalles = db.queryForList(
                        "SELECT codigo_insumo, cantidad_necesaria FROM recetas_detalle WHERE id_producto = ?",
                        p.getId()
                );
                Receta recetaObj = new Receta();
                for (Map<String, Object> det : detalles) {
                    recetaObj.getIngredientes().put(
                            (String) det.get("codigo_insumo"),
                            ((Number) det.get("cantidad_necesaria")).doubleValue()
                    );
                }
                p.setReceta(recetaObj);
            }
            return p;
        });
    }

    @Transactional
    public boolean actualizarProducto(Producto p) {
        String sql = "UPDATE productos SET nombre = ?, precio = ?, categoria = ?, stock_directo = ?, imagen_base64 = ?, atributos_extra = ?::jsonb WHERE id_producto = ?";
        String jsonAtributos = serializarAtributos(p);

        boolean exito = db.update(
                sql,
                p.getNombre(),
                p.getPrecio(),
                p.getCategoria(),
                (int) p.getStock(),
                p.getImagenURL(),
                jsonAtributos,
                p.getId()
        ) > 0;

        if (exito) {
            notificarSyncCatalogo();
        }
        return exito;
    }

    @Transactional
    public boolean eliminarProducto(int idProducto) {
        boolean exito = db.update("UPDATE productos SET estado = 'Inactivo' WHERE id_producto = ?", idProducto) > 0;
        if (exito) {
            notificarSyncCatalogo();
        }
        return exito;
    }

    @Transactional
    public boolean actualizarImagenProducto(int idProducto, String nuevaURL) {
        boolean exito = db.update("UPDATE productos SET imagen_base64 = ? WHERE id_producto = ?", nuevaURL, idProducto) > 0;
        if (exito) {
            notificarSyncCatalogo();
        }
        return exito;
    }

    private String serializarAtributos(Producto p) {
        Map<String, String> attrs = new HashMap<>();
        if (p.getAtributosDinamicos() != null) {
            attrs.putAll(p.getAtributosDinamicos());
        }

        if ("Promocion".equalsIgnoreCase(p.getCategoria())) {
            attrs.remove("tamano");
        } else {
            String tamano = attrs.get("tamano");
            if (tamano == null || tamano.trim().isEmpty() || tamano.equalsIgnoreCase("Sin tamaño")) {
                attrs.remove("tamano");
            } else {
                attrs.put("tamano", tamano.trim());
            }
        }

        p.setAtributosDinamicos(attrs);

        try {
            return mapper.writeValueAsString(attrs);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void notificarSyncCatalogo() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                socketHandler.notificarAClientes("{\"evento\": \"SYNC_CATALOGO\"}");
            }
        });
    }
}