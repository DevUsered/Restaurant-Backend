package AttizosBackend.controller;

import AttizosBackend.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cocina")
public class PedidoController {
    @Autowired
    private PedidoService pedidoService;

    @GetMapping("/pendientes")
    public List<Map<String, Object>> obtenerPendientes() {
        return pedidoService.obtenerPedidosPendientes();
    }
    @GetMapping("/pedidos/{id}/detalles")
    public List<String> obtenerDetalles(@PathVariable int id) {
        return pedidoService.obtenerDetallesParaCocina(id);
    }

    @DeleteMapping("/pedidos/{id}/despachar")
    public boolean despacharPedido(@PathVariable int id) {
        return pedidoService.eliminarPedidoDespachado(id);
    }
    @PutMapping("/pedidos/{id}/cancelar")
    public boolean cancelarPedido(@PathVariable int id){
        return pedidoService.cancelarPedidoYAnularVenta(id);
    }
}
