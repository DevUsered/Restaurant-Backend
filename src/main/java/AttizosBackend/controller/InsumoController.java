package AttizosBackend.controller;

import AttizosBackend.model.Insumo;
import AttizosBackend.service.InsumoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/insumos")
public class InsumoController {

    @Autowired
    private InsumoService insumoService;

    @GetMapping
    public List<Insumo> obtenerTodosInsumos() {
        return insumoService.obtenerInventarioActivo();
    }

    @GetMapping("/siguiente-codigo")
    public String obtenerSiguienteCodigo() {
        return insumoService.generarSiguienteCodigo();
    }

    // Recibimos el costoInicial por la URL como parámetro: /api/insumos?costoInicial=15.5
    @PostMapping
    public boolean crearInsumo(@RequestBody Insumo insumo, @RequestParam double costoInicial) {
        return insumoService.insertarInsumoNuevo(insumo, costoInicial);
    }

    @PutMapping("/{codigo}/inactivar")
    public boolean inactivarInsumo(@PathVariable String codigo) {
        return insumoService.darDeBajaInsumo(codigo);
    }

    @PostMapping("/{codigo}/lotes")
    public boolean registrarLote(@PathVariable String codigo, @RequestParam double cantidad, @RequestParam double costo, @RequestParam String vencimiento) {
        return insumoService.registrarNuevaCompraLote(codigo, cantidad, costo, LocalDate.parse(vencimiento));
    }

    @PutMapping("/{codigo}/vencidos")
    public double darDeBajaVencidos(@PathVariable String codigo) {
        return insumoService.darDeBajaLotesVencidos(codigo);
    }
    @PutMapping("/{codigo}/descontar")
    public boolean descontarStock(@PathVariable String codigo, @RequestParam double cantidad) {
        try {
            return insumoService.descontarStockFEFO(codigo, cantidad);
        } catch (RuntimeException e) {
            return false;
        }
    }
}