package AttizosBackend.controller;

import AttizosBackend.model.VentaRequest;
import AttizosBackend.service.FacturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ventas")
public class FacturaController {
    @Autowired
    private FacturaService facturaService;

    @PostMapping
    public Map<String, Integer> registrarVenta(@RequestBody VentaRequest request) {
        return facturaService.registrarVenta(request);
    }

    @PutMapping("/{id}/anular")
    public boolean anularVenta(@PathVariable int id) {
        return facturaService.anularVenta(id);
    }
    @GetMapping("/{id}")
    public Map<String, Object> obtenerFactura(@PathVariable int id) {
        return facturaService.obtenerFacturaConDetalles(id);
    }
}
