package AttizosBackend.controller;

import AttizosBackend.model.Empleado;
import AttizosBackend.service.EmpleadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empleados")
public class EmpleadoController {

    @Autowired
    private EmpleadoService empleadoService;

    @GetMapping
    public List<Empleado> listarActivos() {
        return empleadoService.obtenerEmpleadosActivos();
    }

    @PostMapping
    public Empleado crearEmpleado(@RequestBody Empleado empleado) {
        return empleadoService.guardarEmpleado(empleado);
    }

    @PutMapping
    public Empleado actualizarEmpleado(@RequestBody Empleado empleado) {
        return empleadoService.actualizarEmpleado(empleado);
    }

    @PutMapping("/{id}/inactivar")
    public void inactivarEmpleado(@PathVariable String id) {
        empleadoService.inactivarEmpleado(id);
    }

    @PutMapping("/{id}/pago")
    public void registrarFechaPago(@PathVariable String id) {
        empleadoService.registrarFechaPago(id);
    }
}