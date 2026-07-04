package AttizosBackend.model;

public class DetalleReceta {
    private String codigoInsumo;
    private double cantidad;

    public DetalleReceta() {}

    public String getCodigoInsumo() { return codigoInsumo; }
    public void setCodigoInsumo(String codigoInsumo) { this.codigoInsumo = codigoInsumo; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
}