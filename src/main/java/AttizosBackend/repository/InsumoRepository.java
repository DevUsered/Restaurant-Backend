package AttizosBackend.repository;

import AttizosBackend.model.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, String> {

    List<Insumo> findByEstado(String estado);


    @Modifying
    @Transactional
    @Query("UPDATE Insumo i SET i.nombre = :nombre, i.categoria = :categoria, i.unidad = :unidad, i.stockMinimo = :min, i.stockMaximo = :max WHERE i.codigo = :codigo")
    int actualizarInsumoDirecto(@Param("nombre") String nombre, @Param("categoria") String categoria, @Param("unidad") String unidad, @Param("min") double min, @Param("max") double max, @Param("codigo") String codigo);

    @Modifying
    @Transactional
    @Query("UPDATE Insumo i SET i.estado = 'Inactivo' WHERE i.codigo = :codigo")
    int inactivarInsumoDirecto(@Param("codigo") String codigo);
}
