package AttizosBackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/web/cocina")
@CrossOrigin(origins = "*") // Permite que cualquier navegador/tablet de tu red local se conecte sin bloqueos de seguridad
public class CocinaWebController {

    @Autowired
    private JdbcTemplate db;

    /**
     * Endpoint para el Login de la Tablet web
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginCocina(@RequestBody Map<String, String> credenciales) {
        String username = credenciales.get("username");
        String passwordHash = credenciales.get("password"); // Se asume que la web enviará el hash o la contraseña como lo manejes

        if (username == null || passwordHash == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("exito", false, "mensaje", "Faltan credenciales."));
        }

        String sql = "SELECT id_empleado, nombre, cargo, estado FROM empleados WHERE username = ? AND password_hash = ? AND estado = 'Activo'";
        List<Map<String, Object>> usuarios = db.queryForList(sql, username, passwordHash);

        if (usuarios.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("exito", false, "mensaje", "Usuario o contraseña incorrectos."));
        }

        Map<String, Object> usuario = usuarios.get(0);
        String cargo = (String) usuario.get("cargo");

        if (cargo == null || (!cargo.equalsIgnoreCase("Cocinero") && !cargo.equalsIgnoreCase("Chef"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("exito", false, "mensaje", "⛔ Acceso denegado. Pantalla exclusiva para personal de cocina."));
        }

        return ResponseEntity.ok(Map.of(
                "exito", true,
                "mensaje", "Bienvenido a la cocina, " + usuario.get("nombre"),
                "usuario", Map.of(
                        "id_empleado", usuario.get("id_empleado"),
                        "nombre", usuario.get("nombre"),
                        "cargo", cargo
                )
        ));
    }
}