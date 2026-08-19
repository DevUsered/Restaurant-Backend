package AttizosBackend.controller;

import AttizosBackend.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/web/pedidos") // Le ponemos "web" para diferenciar de tus APIs internas
@CrossOrigin(origins = "*")
public class PedidoWebController {

    @Autowired
    private PedidoService pedidoService; // Reutilizamos tu excelente servicio existente

    /**
     * Endpoint 1: La tablet web pide la lista de pedidos pendientes
     */
    @GetMapping("/pendientes")
    public ResponseEntity<List<Map<String, Object>>> obtenerPedidosPendientes() {
        // 1. Obtenemos los pedidos base (ID, Ticket, Cliente, Estado) usando tu servicio
        List<Map<String, Object>> pedidos = pedidoService.obtenerPedidosPendientes();

        // 2. Por cada pedido, le inyectamos los detalles (platos)
        for (Map<String, Object> pedido : pedidos) {
            int idPedido = (Integer) pedido.get("idPedido");

            // Reutilizamos tu método que ya devuelve la lista en formato "2x Hamburguesa"
            List<String> detalles = pedidoService.obtenerDetallesParaCocina(idPedido);

            if (detalles == null || detalles.isEmpty()) {
                pedido.put("detalles", List.of("Sin detalles de preparación"));
            } else {
                pedido.put("detalles", detalles);
            }
        }

        return ResponseEntity.ok(pedidos);
    }

    /**
     * Endpoint 2: El cocinero toca el botón "Despachar" en la tablet
     */
    @PostMapping("/despachar/{idPedido}")
    public ResponseEntity<?> despacharPedido(@PathVariable int idPedido) {

        // Reutilizamos tu método que borra de cola_cocina, marca factura Finalizada y notifica por WebSockets
        boolean exito = pedidoService.eliminarPedidoDespachado(idPedido);

        if (exito) {
            return ResponseEntity.ok(Map.of("exito", true, "mensaje", "Pedido despachado correctamente"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("exito", false, "mensaje", "No se pudo despachar el pedido. Verifique si aún existe."));
        }
    }
}