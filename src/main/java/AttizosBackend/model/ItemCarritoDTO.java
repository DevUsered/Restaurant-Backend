package AttizosBackend.model;

public class ItemCarritoDTO {
    private int idProducto;
    private int cantidad;
    private double precio;
    private boolean tieneReceta;

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }
    public boolean isTieneReceta() { return tieneReceta; }
    public void setTieneReceta(boolean tieneReceta) { this.tieneReceta = tieneReceta; }
}
