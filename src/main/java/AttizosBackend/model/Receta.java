package AttizosBackend.model;

import java.util.HashMap;
import java.util.Map;

public class Receta {
    private Map<String, Double> ingredientes = new HashMap<>();
    public Receta() {}

    public Map<String, Double> getIngredientes() {
        return ingredientes;
    }

    public void setIngredientes(Map<String, Double> ingredientes) {
        this.ingredientes = ingredientes;
    }
}