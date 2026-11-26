package AttizosBackend.qr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "qr.proveedor", havingValue = "simulador", matchIfMissing = true)
public class SimuladorQrService implements ProveedorQrService {

    @Value("${qr.simulador.segundos-auto-pago:0}")
    private int segundosAutoPago;

    private record RegistroSimulado(Instant creadoEn, boolean pagadoForzado, boolean cancelado) {}

    private final Map<String, RegistroSimulado> qrsSimulados = new ConcurrentHashMap<>();

    @Override
    public QrGenerado generarQr(double monto, String glosa, String datosAdicionales) {
        String qrId = "SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        qrsSimulados.put(qrId, new RegistroSimulado(Instant.now(), false, false));

        String imagenBase64 = generarImagenQrPrueba(qrId + "|" + monto + "|" + glosa);
        return new QrGenerado(true, qrId, imagenBase64, "QR simulado generado");
    }

    @Override
    public EstadoQr consultarEstado(String qrId) {
        RegistroSimulado reg = qrsSimulados.get(qrId);
        if (reg == null || reg.cancelado()) {
            return new EstadoQr(EstadoPago.EXPIRADO, null, "QR cancelado o inexistente");
        }

        boolean cumplidoTiempo = segundosAutoPago > 0
                && Instant.now().isAfter(reg.creadoEn().plusSeconds(segundosAutoPago));

        if (reg.pagadoForzado() || cumplidoTiempo) {
            qrsSimulados.remove(qrId);
            String voucher = "VCH-" + (100000 + new Random().nextInt(900000));
            return new EstadoQr(EstadoPago.PAGADO, voucher, "Pago simulado confirmado");
        }

        return new EstadoQr(EstadoPago.PENDIENTE, null, "Esperando pago simulado...");
    }

    @Override
    public boolean cancelarQr(String qrId) {
        RegistroSimulado actual = qrsSimulados.get(qrId);
        if (actual != null) {
            qrsSimulados.put(qrId, new RegistroSimulado(actual.creadoEn(), false, true));
            return true;
        }
        return false;
    }

    public void forzarPago(String qrId) {
        RegistroSimulado actual = qrsSimulados.get(qrId);
        if (actual != null && !actual.cancelado()) {
            qrsSimulados.put(qrId, new RegistroSimulado(actual.creadoEn(), true, false));
        }
    }

    private String generarImagenQrPrueba(String semilla) {
        try {
            int modulos = 25;
            int escala = 12;
            int margen = 2;
            int tamano = (modulos + margen * 2) * escala;

            BufferedImage img = new BufferedImage(tamano, tamano, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, tamano, tamano);
            g.setColor(new Color(17, 17, 17));

            Random rnd = new Random(semilla.hashCode());
            for (int y = 0; y < modulos; y++) {
                for (int x = 0; x < modulos; x++) {
                    boolean enEsquina = (x < 7 && y < 7)
                            || (x >= modulos - 7 && y < 7)
                            || (x < 7 && y >= modulos - 7);
                    if (!enEsquina && rnd.nextBoolean()) {
                        g.fillRect((x + margen) * escala, (y + margen) * escala, escala, escala);
                    }
                }
            }

            dibujarPatronPosicion(g, margen, margen, escala);
            dibujarPatronPosicion(g, margen + modulos - 7, margen, escala);
            dibujarPatronPosicion(g, margen, margen + modulos - 7, escala);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    private void dibujarPatronPosicion(Graphics2D g, int gx, int gy, int escala) {
        g.setColor(new Color(17, 17, 17));
        g.fillRect(gx * escala, gy * escala, 7 * escala, 7 * escala);
        g.setColor(Color.WHITE);
        g.fillRect((gx + 1) * escala, (gy + 1) * escala, 5 * escala, 5 * escala);
        g.setColor(new Color(17, 17, 17));
        g.fillRect((gx + 2) * escala, (gy + 2) * escala, 3 * escala, 3 * escala);
    }
}