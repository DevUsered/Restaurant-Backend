package AttizosBackend.controller;

import AttizosBackend.model.Reserva;
import AttizosBackend.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @GetMapping("/pendientes")
    public List<Reserva> obtenerPendientes() {
        return reservaService.obtenerReservasPendientesYLimpiar();
    }

    @PostMapping
    public boolean crearReserva(@RequestBody Reserva reserva) {
        return reservaService.insertarReserva(reserva);
    }

    @PutMapping("/{id}/estado")
    public boolean cambiarEstado(@PathVariable String id, @RequestParam String estado) {
        return reservaService.actualizarEstadoReserva(id, estado);
    }
}