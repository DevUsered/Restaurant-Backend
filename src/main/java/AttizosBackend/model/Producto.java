package AttizosBackend.model;

import java.util.HashMap;
import java.util.Map;

public class Producto {
    private int id;
    private String nombre;
    private double precio;
    private String categoria;
    private double stock;
    private boolean tieneReceta;
    private String imagenURL;
    private String estado = "Activo";
    private Receta receta;

    // Aquí recibiremos los campos dinámicos (tamaño, sabor, etc.)
    private Map<String, String> atributosDinamicos = new HashMap<>();

    public Producto() {}

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public double getStock() { return stock; }
    public void setStock(double stock) { this.stock = stock; }

    public boolean tieneReceta() { return tieneReceta; }
    public void setTieneReceta(boolean tieneReceta) { this.tieneReceta = tieneReceta; }

    public String getImagenURL() { return imagenURL; }
    public void setImagenURL(String imagenURL) { this.imagenURL = imagenURL; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public boolean isTieneReceta() {
        return tieneReceta;
    }

    public Receta getReceta() {
        return receta;
    }

    public void setReceta(Receta receta) {
        this.receta = receta;
    }

    public Map<String, String> getAtributosDinamicos() { return atributosDinamicos; }
    public void setAtributosDinamicos(Map<String, String> atributosDinamicos) { this.atributosDinamicos = atributosDinamicos; }
}