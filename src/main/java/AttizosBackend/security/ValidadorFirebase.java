package AttizosBackend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;


import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class ValidadorFirebase {

    private static final String FIREBASE_URL = "https://licencias-codecraft-default-rtdb.firebaseio.com/licencias/";

    public static String NOMBRE_RESTAURANTE_OFICIAL = "Restaurante Demo";
    public static boolean SISTEMA_ACTIVADO = false;

    @PostConstruct
    public void validarAlArrancar() {
        System.out.println("🔒 [Seguridad] Iniciando verificación de licencia remota...");

        String idHardware = HardwareUtil.obtenerIDPlacaMadre();
        String idCorto = generarIDCorto(idHardware);

        boolean exitoNube = consultarFirebase(idCorto, idHardware);

        if (!exitoNube) {
            System.out.println("🌐 Servidor offline o sin respuesta. Validando en modo local...");
            boolean exitoLocal = leerLicenciaLocal(idHardware);

            if (!exitoLocal) {
                bloquearSistema(idCorto);
            }
        } else {
            SISTEMA_ACTIVADO = true;
        }
    }

    private boolean consultarFirebase(String idCorto, String idHardwareReal) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FIREBASE_URL + idCorto + ".json"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null && !response.body().equals("null")) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode datosNube = mapper.readTree(response.body());

                String estado = datosNube.get("estado").asText();
                String vencimientoStr = datosNube.get("vencimiento").asText();
                String restaurante = datosNube.get("restaurante").asText();

                if ("ACTIVO".equalsIgnoreCase(estado)) {
                    LocalDate fechaVencimiento = LocalDate.parse(vencimientoStr);

                    if (!LocalDate.now().isAfter(fechaVencimiento)) {
                        NOMBRE_RESTAURANTE_OFICIAL = restaurante;
                        System.out.println("Licencia en línea verificada con éxito.");
                        System.out.println("Restaurante Autorizado: " + NOMBRE_RESTAURANTE_OFICIAL);

                        verificarAvisoVencimiento(fechaVencimiento);

                        guardarLicenciaLocal(idHardwareReal, vencimientoStr, NOMBRE_RESTAURANTE_OFICIAL);
                        return true;
                    } else {
                        System.err.println("[Firebase] ALERTA DE SEGURIDAD: LICENCIA VENCIDA en la nube.");
                        bloquearSistema(idCorto);
                    }
                } else {
                    System.err.println("[Firebase] ALERTA DE SEGURIDAD: Licencia suspendida o inactiva.");
                    bloquearSistema(idCorto);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo establecer conexión con Firebase: " + e.getMessage());
        }
        return false;
    }

    private void guardarLicenciaLocal(String idHardware, String vencimiento, String restaurante) {
        String rutaCarpeta = System.getenv("APPDATA") + File.separator + "Attizos";
        File archivoLocal = new File(rutaCarpeta, "licencia_local.dat");

        // Estructura interna: ID Hardware | Fecha Vencimiento | Nombre
        String datosPlano = idHardware + "|" + vencimiento + "|" + restaurante;
        String datosCifrados = CriptografiaUtil.encriptar(datosPlano);

        if (datosCifrados != null) {
            try (FileWriter fw = new FileWriter(archivoLocal)) {
                fw.write(datosCifrados);
                System.out.println("💾 Caché de licencia local actualizada correctamente.");
            } catch (Exception e) {
                System.err.println("Error escribiendo archivo de licencia local: " + e.getMessage());
            }
        }
    }

    private boolean leerLicenciaLocal(String idHardwareReal) {
        String rutaArchivo = System.getenv("APPDATA") + File.separator + "Attizos" + File.separator + "licencia_local.dat";
        File archivoLocal = new File(rutaArchivo);

        if (!archivoLocal.exists()) {
            System.err.println("❌ Error: No existe registro de activación offline local.");
            return false;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(archivoLocal))) {
            String datosCifrados = br.readLine();
            String datosPlanos = CriptografiaUtil.desencriptar(datosCifrados);

            if (datosPlanos == null) {
                System.err.println("❌ Error: Los datos de la licencia local fueron alterados o están corruptos.");
                return false;
            }

            String[] partes = datosPlanos.split("\\|");
            if (partes.length == 3) {
                String idHardwareGuardado = partes[0];
                LocalDate fechaVencimiento = LocalDate.parse(partes[1]);
                String restauranteGuardado = partes[2];

                // Validamos que el archivo pertenezca a ESTA computadora
                if (idHardwareReal.equals(idHardwareGuardado)) {
                    // Validamos que la fecha no haya expirado
                    if (!LocalDate.now().isAfter(fechaVencimiento)) {
                        NOMBRE_RESTAURANTE_OFICIAL = restauranteGuardado;
                        SISTEMA_ACTIVADO = true;
                        System.out.println("✅ [Offline] Sistema autorizado localmente hasta: " + fechaVencimiento);
                        return true;
                    } else {
                        System.err.println("⏳ [Offline] Alerta: La licencia local almacenada ha vencido.");
                    }
                } else {
                    System.err.println("🛑 [Offline] Alerta de seguridad: Intento de clonación de licencia detectado.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando validación offline: " + e.getMessage());
        }
        return false;
    }

    private String generarIDCorto(String hardwareID) {
        int hash = Math.abs(hardwareID.hashCode());
        return "ATZ-" + String.format("%04X", hash).toUpperCase();
    }

    private void bloquearSistema(String idCorto) {
        System.err.println("==========================================================");
        System.err.println("❌ ERROR: EL SISTEMA SE ENCUENTRA BLOQUEADO");
        System.err.println("Para habilitar la aplicación, proporcione el siguiente ID:");
        System.err.println("CÓDIGO DE EQUIPO: " + idCorto);
        System.err.println("==========================================================");
        System.exit(0); 
    }
    private void verificarAvisoVencimiento(LocalDate fechaVencimiento){
        long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);
        if(diasRestantes <= 7 && diasRestantes > 0){
            System.out.println("⚠️ ALERTA_VENCIMIENTO: " + diasRestantes + "|" + fechaVencimiento.toString());
        }else if(diasRestantes <= 0){
            System.out.println("⚠️ ALERTA_VENCIMIENTO: 0|" + fechaVencimiento.toString());
        }
    }
}