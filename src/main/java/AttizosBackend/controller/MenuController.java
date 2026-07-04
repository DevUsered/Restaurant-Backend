package AttizosBackend.controller;

import AttizosBackend.model.Producto;
import AttizosBackend.model.Promocion;
import AttizosBackend.service.ProductoService;
import AttizosBackend.service.PromocionService;
import AttizosBackend.service.RecetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menu")
public class MenuController {
    @Autowired
    private RecetaService recetaService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private PromocionService promocionService;

    @GetMapping("/productos")
    public List<Producto> obtenerMenu() {
        return productoService.obtenerMenuCompleto();
    }

    @PostMapping("/productos")
    public Producto crearProducto(@RequestBody Producto producto) {
        return productoService.insertarProducto(producto);
    }

    @PutMapping("/productos")
    public boolean actualizarProducto(@RequestBody Producto producto) {
        return productoService.actualizarProducto(producto);
    }

    @PutMapping("/productos/{id}/inactivar")
    public boolean inactivarProducto(@PathVariable int id) {
        return productoService.eliminarProducto(id);
    }

    @PutMapping("/productos/{id}/imagen")
    public boolean actualizarImagen(@PathVariable int id, @RequestBody Map<String, String> body) {
        return productoService.actualizarImagenProducto(id, body.get("imagenURL"));
    }

    @GetMapping("/promociones")
    public List<Promocion> obtenerPromociones() {
        return promocionService.cargarPromocionesActivas();
    }

    @GetMapping("/promociones/{id}/detalles")
    public List<Map<String, Object>> obtenerDetallesPromocion(@PathVariable int id) {
        return promocionService.obtenerDetallesDePromocion(id);
    }

    @PostMapping("/promociones")
    public Promocion crearPromocion(@RequestBody Promocion promocion) {
        return promocionService.guardarNuevaPromocion(promocion);
    }

    @PutMapping("/promociones/verificar-caducidad")
    public int verificarCaducidadPromociones() {
        return promocionService.verificarYDesactivarPromociones();
    }

    @PostMapping("/productos/{id}/receta")
    public boolean guardarReceta(@PathVariable int id, @RequestBody AttizosBackend.model.Receta receta) {
        return recetaService.guardarReceta(id, receta);
    }
}