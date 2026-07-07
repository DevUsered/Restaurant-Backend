package AttizosBackend.security;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class HardwareUtil {
    public static String obtenerIDPlacaMadre() {
        try {
            Process p = Runtime.getRuntime().exec("wmic baseboard get serialnumber");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            String result = "";
            while ((line = input.readLine()) != null) {
                result += line;
            }
            input.close();
            // Quitamos la cabecera "SerialNumber" y espacios en blanco
            String idLimpio = result.replace("SerialNumber", "").replaceAll("\\s+", "").trim();
            return idLimpio.isEmpty() ? "SERVER-DEFAULT-ID" : idLimpio;
        } catch (Exception e) {
            return "SERVER-DEFAULT-ID";
        }
    }
}