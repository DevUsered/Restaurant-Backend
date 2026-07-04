package AttizosBackend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "insumos_catalogo")
public class Insumo {

    @Column(name = "estado", length = 20)
    private String estado = "Activo";

    @Transient
    @JsonProperty("stockActual")
    private double stockActual;

    @Transient
    @JsonProperty("fechaVencimiento")
    private java.time.LocalDate fechaVencimiento;
    @Id
    @Column(name = "codigo", length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "categoria", length = 50)
    private String categoria;

    @Column(name = "unidad_medida", length = 20)
    private String unidad;

    @Column(name = "stock_minimo")
    private double stockMinimo;

    @Column(name = "stock_maximo")
    private double stockMaximo;


    public Insumo() {}

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public double getStockMaximo() {
        return stockMaximo;
    }

    public void setStockMaximo(double stockMaximo) {
        this.stockMaximo = stockMaximo;
    }

    public double getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(double stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
    public double getStockActual() {
        return stockActual;
    }

    public void setStockActual(double stockActual) {
        this.stockActual = stockActual;
    }

    public java.time.LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(java.time.LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }
}
