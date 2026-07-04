package AttizosBackend.controller;

import AttizosBackend.model.InsumoLote;
import AttizosBackend.service.InsumoLoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lotes")
public class InsumoLoteController {

    @Autowired
    private InsumoLoteService loteService;

    @GetMapping("/insumo/{codigo}")
    public List<InsumoLote> listarLotesDeInsumo(@PathVariable String codigo) {
        return loteService.obtenerLotesPorInsumo(codigo);
    }

    @GetMapping("/alertas")
    public List<InsumoLote> alertasPorVencer(@RequestParam(defaultValue = "15") int dias) {
        return loteService.obtenerLotesPorVencer(dias);
    }
}