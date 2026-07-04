package AttizosBackend.service;

import AttizosBackend.model.Receta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RecetaService {

    @Autowired
    private JdbcTemplate db;

    @Transactional
    public boolean guardarReceta(int idProducto, Receta receta) {
        db.update("DELETE FROM recetas_detalle WHERE id_producto = ?", idProducto);

        if (receta.getIngredientes() == null || receta.getIngredientes().isEmpty()) {
            return true;
        }

        String sql = "INSERT INTO recetas_detalle (id_producto, codigo_insumo, cantidad_necesaria) VALUES (?, ?, ?)";

        List<Map.Entry<String, Double>> listaIngredientes = new ArrayList<>(receta.getIngredientes().entrySet());

        db.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, idProducto);
                ps.setString(2, listaIngredientes.get(i).getKey());
                ps.setDouble(3, listaIngredientes.get(i).getValue()); // cantidad_necesaria
            }

            @Override
            public int getBatchSize() {
                return listaIngredientes.size();
            }
        });
        return true;
    }
}