package AttizosBackend.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "empleados")

public class Empleado {
    @Id
    @Column(name = "id_empleado", length = 50)
    private String idEmpleado;
    @Column(name = "nombre", length = 50)
    private String nombre;

    @Column(name = "cargo", nullable = false, length = 50)
    private String cargo;

    @Column(name = "sueldo", nullable = false)
    private double sueldo;

    @Column(name = "estado", length = 20)
    private String estado = "Activo";

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "fecha_ultimo_pago")
    private LocalDate fechaUltimoPago;

    @Column(name = "fecha_contrato")
    private LocalDate fechaContrato;

    public Empleado(){}

    public String getIdEmpleado() {
        return idEmpleado;
    }

    public void setIdEmpleado(String idEmpleado) {
        this.idEmpleado = idEmpleado;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public double getSueldo() {
        return sueldo;
    }

    public void setSueldo(double sueldo) {
        this.sueldo = sueldo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDate getFechaUltimoPago() {
        return fechaUltimoPago;
    }

    public void setFechaUltimoPago(LocalDate fechaUltimoPago) {
        this.fechaUltimoPago = fechaUltimoPago;
    }
    public LocalDate getFechaContrato() {
        return fechaContrato;
    }

    public void setFechaContrato(LocalDate fechaContrato) {
        this.fechaContrato = fechaContrato;
    }
}
