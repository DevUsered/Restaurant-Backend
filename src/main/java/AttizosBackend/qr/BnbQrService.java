package AttizosBackend.qr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "qr.proveedor", havingValue = "bnb")
public class BnbQrService implements ProveedorQrService {

    @Value("${bnb.api.auth-url}")
    private String authUrl;

    @Value("${bnb.api.qr-base-url}")
    private String qrBaseUrl;

    @Value("${bnb.api.account-id}")
    private String accountId;

    @Value("${bnb.api.authorization-id}")
    private String authorizationId;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    private volatile String jwtToken;
    private volatile Instant tokenExpiraEn = Instant.EPOCH;

    @Override
    public QrGenerado generarQr(double monto, String glosa, String datosAdicionales) {
        try {
            String fechaExpiracion = LocalDate.now().plusDays(1).toString();
            String glosaLimpia = escaparJson(glosa != null && !glosa.isBlank() ? glosa : "Consumo Craft POS");

            String body = String.format(
                    Locale.US,
                    "{\"currency\":\"BOB\",\"gloss\":\"%s\",\"amount\":%.2f,\"singleUse\":true,"+
                    "\"expirationDate\":\"%s\", \"additionalData\":\"%s\",\"destinationAccountId\":1}",
                    glosaLimpia, monto, fechaExpiracion, escaparJson(datosAdicionales != null ? datosAdicionales : "Craft POS")
            );

            HttpResponse<String> resp = enviarPostAutenticado(qrBaseUrl + "/getQRWithImageAsync", body);
            System.out.println("[BNB QR] HTTP " + resp.statusCode() + " -> " + resp.body());

            Map<String, Object> json = jsonParser.parseMap(resp.body());

            boolean exito = Boolean.TRUE.equals(json.get("success"));
            Object qrObj = json.get("qr");
            if (qrObj == null && exito) {
                qrObj = json.get("message");
            }

            if (exito && qrObj != null && !String.valueOf(qrObj).isBlank()) {
                Object idObj = json.get("id");
                String qrId = (idObj != null && !"null".equals(String.valueOf(idObj)))
                        ? String.valueOf(idObj)
                        : "BNB-" + System.currentTimeMillis();

                return new QrGenerado(true, qrId, String.valueOf(qrObj), "QR generado correctamente");
            }

            String msg = String.valueOf(json.getOrDefault("message", "No se pudo generar el QR en BNB"));
            return new QrGenerado(false, null, null, msg);

        } catch (Exception e) {
            String detalle = (e.getMessage() != null && !e.getMessage().isBlank())
                    ? e.getMessage()
                    : e.getClass().getSimpleName();
            System.err.println("❌ Error BNB generarQr: " + detalle);
            e.printStackTrace();
            return new QrGenerado(false, null, null, "Error BNB: " + detalle);
        }
    }
    

    @Override
    public EstadoQr consultarEstado(String qrId) {
        try {
            String body = "{\"qrId\":\"" + escaparJson(qrId) + "\"}";
            HttpResponse<String> resp = enviarPostAutenticado(qrBaseUrl + "/getQRStatusAsync", body);
            Map<String, Object> json = jsonParser.parseMap(resp.body());

            // En la API v2 del BNB, el código de estado viene en "qrId" (1=No usado, 2=Usado, 3=Expirado, 4=Error)
            Object statusObj = json.get("qrId") != null ? json.get("qrId") : json.get("statusId");
            int estadoCodigo = parsearEntero(statusObj, 1);
            String mensaje = String.valueOf(json.getOrDefault("message", ""));

            return switch (estadoCodigo) {
                case 2 -> {
                    Object idObj = json.get("id");
                    String comprobante = (idObj != null && !"null".equals(String.valueOf(idObj)))
                            ? String.valueOf(idObj)
                            : qrId;
                    yield new EstadoQr(EstadoPago.PAGADO, comprobante, "Pago confirmado por BNB");
                }
                case 3 -> new EstadoQr(EstadoPago.EXPIRADO, null, "El QR ha expirado");
                case 4 -> new EstadoQr(EstadoPago.ERROR, null, "Error en QR: " + mensaje);
                default -> new EstadoQr(EstadoPago.PENDIENTE, null, "Pendiente de pago");
            };

        } catch (Exception e) {
            return new EstadoQr(EstadoPago.PENDIENTE, null, "Reintentando consulta: " + e.getMessage());
        }
    }

    @Override
    public boolean cancelarQr(String qrId) {
        return true;
    }

    private HttpResponse<String> enviarPostAutenticado(String url, String jsonBody) throws Exception {
        String token = obtenerTokenValido(false);
        HttpResponse<String> resp = ejecutarPost(url, jsonBody, token);

        if (resp.statusCode() == 401) {
            token = obtenerTokenValido(true);
            resp = ejecutarPost(url, jsonBody, token);
        }
        return resp;
    }

    private HttpResponse<String> ejecutarPost(String url, String jsonBody, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private synchronized String obtenerTokenValido(boolean forzarRenovacion) throws Exception {
        if (!forzarRenovacion && jwtToken != null && Instant.now().isBefore(tokenExpiraEn)) {
            return jwtToken;
        }

        String authBody = String.format(
                "{\"accountId\":\"%s\",\"authorizationId\":\"%s\"}",
                escaparJson(accountId),
                escaparJson(authorizationId)
        );

        HttpResponse<String> resp = ejecutarPost(authUrl, authBody, null);
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("Autenticación BNB fallida (HTTP " + resp.statusCode() + ": "+resp.body());
        }

        Map<String, Object> json = jsonParser.parseMap(resp.body());
        if (!Boolean.TRUE.equals(json.get("success")) || json.get("message") == null) {
            throw new IllegalStateException("BNB no devolvió token válido: " + resp.body());
        }

        jwtToken = String.valueOf(json.get("message"));
        tokenExpiraEn = Instant.now().plus(Duration.ofMinutes(50));
        return jwtToken;
    }

    private int parsearEntero(Object valor, int defecto) {
        if (valor instanceof Number num) {
            return num.intValue();
        }
        if (valor != null) {
            try {
                return Integer.parseInt(String.valueOf(valor).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defecto;
    }

    private String escaparJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}