package AttizosBackend.qr;

public interface ProveedorQrService {

    enum EstadoPago {
        PENDIENTE,
        PAGADO,
        EXPIRADO,
        ERROR
    }

    record QrGenerado(
            boolean exito,
            String qrId,
            String imagenBase64,
            String mensaje
    ) {}

    record EstadoQr(
            EstadoPago estado,
            String voucherId,
            String mensaje
    ) {}

    QrGenerado generarQr(double monto, String glosa, String datosAdicionales);

    EstadoQr consultarEstado(String qrId);

    boolean cancelarQr(String qrId);
}