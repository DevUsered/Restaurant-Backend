package AttizosBackend.controller;

import org.springframework.web.bind.annotation.GetMapping;

public class TestController {
    @GetMapping("/hola")
    public String decirHola(){
        return "¡Hola desde el nuevo servidor Spring Boot de Restaurant! La conexión es un éxito.";
    }
}
