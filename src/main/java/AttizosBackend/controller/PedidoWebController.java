package AttizosBackend.controller;

import AttizosBackend.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/web/pedidos")
@CrossOrigin(origins = "*")
public class PedidoWebController {

    @Autowired
    private PedidoService pedidoService;

    @GetMapping("/pendientes")
    public ResponseEntity<List<Map<String, Object>>> obtenerPedidosPendientes() {
        List<Map<String, Object>> pedidos = pedidoService.obtenerPedidosPendientes();

        for (Map<String, Object> pedido : pedidos) {
            int idPedido = (Integer) pedido.get("idPedido");
            List<String> detalles = pedidoService.obtenerDetallesParaCocina(idPedido);

            if (detalles == null || detalles.isEmpty()) {
                pedido.put("detalles", List.of("Sin detalles de preparación"));
            } else {
                pedido.put("detalles", detalles);
            }
        }

        return ResponseEntity.ok(pedidos);
    }

    @PostMapping("/despachar/{idPedido}")
    public ResponseEntity<?> despacharPedido(@PathVariable int idPedido) {
        // Al llamar a eliminarPedidoDespachado, el PedidoService
        // se encarga de borrarlo y de gritarle a JavaFX una sola vez.
        boolean exito = pedidoService.eliminarPedidoDespachado(idPedido);

        if (exito) {
            return ResponseEntity.ok(Map.of("exito", true, "mensaje", "Pedido despachado correctamente"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("exito", false, "mensaje", "No se pudo despachar el pedido. Verifique si aún existe."));
        }
    }
}