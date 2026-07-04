package AttizosBackend.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Promocion extends Producto {
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private List<DetalleCombo> productosCombo = new ArrayList<>();

    public Promocion() {}

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public List<DetalleCombo> getProductosCombo() { return productosCombo; }
    public void setProductosCombo(List<DetalleCombo> productosCombo) { this.productosCombo = productosCombo; }
}