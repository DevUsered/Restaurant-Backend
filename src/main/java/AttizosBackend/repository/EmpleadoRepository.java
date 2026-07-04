package AttizosBackend.repository;

import AttizosBackend.model.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, String> {

    List<Empleado> findByEstado(String estado);

    // 1. Francotirador para Fecha de Pago
    @Modifying
    @Transactional
    @Query("UPDATE Empleado e SET e.fechaUltimoPago = CURRENT_DATE WHERE e.idEmpleado = :id")
    int actualizarFechaPagoDirecto(@Param("id") String id);

    // 2. Francotirador para Inactivar (Despedir)
    @Modifying
    @Transactional
    @Query("UPDATE Empleado e SET e.estado = 'Inactivo' WHERE e.idEmpleado = :id")
    int inactivarEmpleadoDirecto(@Param("id") String id);

    // 3. Francotirador para Actualizar Datos
    @Modifying
    @Transactional
    @Query("UPDATE Empleado e SET e.nombre = :nombre, e.cargo = :cargo, e.sueldo = :sueldo, e.username = :user, e.passwordHash = :pass WHERE e.idEmpleado = :id")
    int actualizarDatosDirecto(@Param("nombre") String nombre, @Param("cargo") String cargo, @Param("sueldo") double sueldo, @Param("user") String user, @Param("pass") String pass, @Param("id") String id);
}