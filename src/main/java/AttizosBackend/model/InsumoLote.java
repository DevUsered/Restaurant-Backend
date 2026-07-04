package AttizosBackend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "insumos_lotes")
public class InsumoLote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lote")
    private Integer idLote;

    @Column(name = "codigo_insumo", nullable = false, length = 50)
    private String codigoInsumo;

    @Column(name = "stock_actual", nullable = false)
    private double stockActual;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso = LocalDate.now();

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "costo_compra")
    private double costoCompra;

    @Column(name = "estado", length = 20)
    private String estado = "Activo";

    public InsumoLote() {}

    // Getters y Setters
    public Integer getIdLote() { return idLote; }
    public void setIdLote(Integer idLote) { this.idLote = idLote; }

    public String getCodigoInsumo() { return codigoInsumo; }
    public void setCodigoInsumo(String codigoInsumo) { this.codigoInsumo = codigoInsumo; }

    public double getStockActual() { return stockActual; }
    public void setStockActual(double stockActual) { this.stockActual = stockActual; }

    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDate fechaIngreso) { this.fechaIngreso = fechaIngreso; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public double getCostoCompra() { return costoCompra; }
    public void setCostoCompra(double costoCompra)  { this.costoCompra = costoCompra; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}