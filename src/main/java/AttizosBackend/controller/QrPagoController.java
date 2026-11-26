package AttizosBackend.controller;

import AttizosBackend.qr.ProveedorQrService.QrGenerado;
import AttizosBackend.qr.QrPagoManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/qr")
@CrossOrigin(origins = "*")
public class QrPagoController {

    @Autowired
    private QrPagoManagerService qrManager;

    @PostMapping("/generar")
    public ResponseEntity<?> generarQr(@RequestBody Map<String, Object> payload) {
        double monto = payload.get("monto") instanceof Number
                ? ((Number) payload.get("monto")).doubleValue()
                : 0.0;

        if (monto <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "exito", false,
                    "mensaje", "El monto debe ser mayor a 0."
            ));
        }

        String glosa = String.valueOf(payload.getOrDefault("glosa", "Consumo Craft POS"));
        String datosAdicionales = String.valueOf(payload.getOrDefault("datosAdicionales", "Craft POS"));

        QrGenerado res = qrManager.iniciarCobroQr(monto, glosa, datosAdicionales);
        if (res.exito()) {
            return ResponseEntity.ok(Map.of(
                    "exito", true,
                    "qrId", res.qrId(),
                    "imagenBase64", res.imagenBase64(),
                    "mensaje", res.mensaje()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "exito", false,
                "mensaje", res.mensaje()
        ));
    }

    @GetMapping("/estado")
    public ResponseEntity<?> consultarEstado() {
        return ResponseEntity.ok(qrManager.obtenerEstadoActual());
    }

    @PostMapping("/cancelar")
    public ResponseEntity<?> cancelarQr() {
        qrManager.cancelarCobroActivo();
        return ResponseEntity.ok(Map.of("exito", true, "mensaje", "Cobro QR cancelado"));
    }

    // Endpoint útil durante pruebas cuando qr.proveedor=simulador
    @PostMapping("/simular-pago")
    public ResponseEntity<?> simularPago() {
        qrManager.simularConfirmacionActual();
        return ResponseEntity.ok(Map.of("exito", true, "mensaje", "Pago simulado disparado"));
    }
}