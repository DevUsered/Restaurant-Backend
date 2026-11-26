package AttizosBackend.qr;

import AttizosBackend.qr.ProveedorQrService.EstadoPago;
import AttizosBackend.qr.ProveedorQrService.EstadoQr;
import AttizosBackend.qr.ProveedorQrService.QrGenerado;
import AttizosBackend.websocket.SyncSocketHandler;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class QrPagoManagerService {

    private static final int INTERVALO_POLLING_MS = 2500;
    private static final int MAX_INTENTOS_POLLING = 120; // ~5 minutos máximo

    @Autowired
    private ProveedorQrService proveedorQr;

    @Autowired
    private SyncSocketHandler socketHandler;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "qr-polling-worker");
        t.setDaemon(true);
        return t;
    });

    private volatile ScheduledFuture<?> tareaPollingActual;
    private volatile String qrIdActivo;
    private volatile double montoActivo;
    private volatile EstadoQr ultimoEstado;

    public synchronized QrGenerado iniciarCobroQr(double monto, String glosa, String datosAdicionales) {
        detenerPollingYLimpiar(true);

        QrGenerado generado = proveedorQr.generarQr(monto, glosa, datosAdicionales);
        if (!generado.exito()) {
            return generado;
        }

        qrIdActivo = generado.qrId();
        montoActivo = monto;
        ultimoEstado = new EstadoQr(EstadoPago.PENDIENTE, null, "Esperando pago");
        boolean esSimulador = proveedorQr instanceof SimuladorQrService;

        String jsonMostrar = String.format(
                Locale.US,
                "{\"evento\":\"MOSTRAR_QR\",\"qrId\":\"%s\",\"monto\":%.2f,\"glosa\":\"%s\",\"imagenBase64\":\"%s\",\"modoSimulador\":%b}",
                escaparJson(generado.qrId()),
                monto,
                escaparJson(glosa != null ? glosa : "Pago con QR"),
                escaparJson(generado.imagenBase64()), 
                esSimulador
        );
        socketHandler.notificarAClientes(jsonMostrar);

        iniciarPolling(generado.qrId(), monto);
        return generado;
    }

    private void iniciarPolling(String qrId, double monto) {
        AtomicInteger intentos = new AtomicInteger(0);

        tareaPollingActual = scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (!qrId.equals(qrIdActivo)) {
                    return;
                }

                if (intentos.incrementAndGet() > MAX_INTENTOS_POLLING) {
                    proveedorQr.cancelarQr(qrId);
                    ultimoEstado = new EstadoQr(EstadoPago.EXPIRADO, null, "Tiempo de espera agotado");
                    socketHandler.notificarAClientes("{\"evento\":\"LIMPIAR_QR\",\"qrId\":\"" + escaparJson(qrId) + "\"}");
                    detenerPollingYLimpiar(false);
                    return;
                }

                EstadoQr estado = proveedorQr.consultarEstado(qrId);
                ultimoEstado = estado;

                if (estado.estado() == EstadoPago.PAGADO) {
                    String jsonConfirmado = String.format(
                            Locale.US,
                            "{\"evento\":\"PAGO_CONFIRMADO\",\"qrId\":\"%s\",\"voucherId\":\"%s\",\"monto\":%.2f}",
                            escaparJson(qrId),
                            escaparJson(estado.voucherId() != null ? estado.voucherId() : qrId),
                            monto
                    );
                    socketHandler.notificarAClientes(jsonConfirmado);
                    detenerPollingYLimpiar(false);

                } else if (estado.estado() == EstadoPago.EXPIRADO || estado.estado() == EstadoPago.ERROR) {
                    socketHandler.notificarAClientes("{\"evento\":\"LIMPIAR_QR\",\"qrId\":\"" + escaparJson(qrId) + "\"}");
                    detenerPollingYLimpiar(false);
                }

            } catch (Exception e) {
                System.err.println("⚠️ Error verificando estado de QR: " + e.getMessage());
            }
        }, INTERVALO_POLLING_MS, INTERVALO_POLLING_MS, TimeUnit.MILLISECONDS);
    }

    public synchronized void cancelarCobroActivo() {
        detenerPollingYLimpiar(true);
        socketHandler.notificarAClientes("{\"evento\":\"LIMPIAR_QR\"}");
    }

    public Map<String, Object> obtenerEstadoActual() {
        if (ultimoEstado == null) {
            return Map.of("estado", "SIN_QR", "voucherId", "", "mensaje", "No hay cobro QR activo");
        }
        return Map.of(
                "qrId", qrIdActivo != null ? qrIdActivo : "",
                "estado", ultimoEstado.estado().name(),
                "voucherId", ultimoEstado.voucherId() != null ? ultimoEstado.voucherId() : "",
                "mensaje", ultimoEstado.mensaje() != null ? ultimoEstado.mensaje() : ""
        );
    }

    public void simularConfirmacionActual() {
        if (qrIdActivo != null && proveedorQr instanceof SimuladorQrService simulador) {
            simulador.forzarPago(qrIdActivo);
        }
    }

    private void detenerPollingYLimpiar(boolean cancelarEnBanco) {
        if (tareaPollingActual != null) {
            tareaPollingActual.cancel(false);
            tareaPollingActual = null;
        }
        if (cancelarEnBanco && qrIdActivo != null) {
            proveedorQr.cancelarQr(qrIdActivo);
        }
        qrIdActivo = null;
        montoActivo = 0;
    }

    private String escaparJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    @PreDestroy
    public void cerrarRecursos() {
        scheduler.shutdownNow();
    }
}