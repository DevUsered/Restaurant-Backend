package AttizosBackend.security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CriptografiaUtil {
    private static final String CLAVE_SECRETA = "DuraznilloEdgar7";

    public static String encriptar(String datos) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(CLAVE_SECRETA.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] datosEncriptados = cipher.doFinal(datos.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(datosEncriptados);
        } catch (Exception e) {
            return null;
        }
    }

    public static String desencriptar(String datosEncriptados) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(CLAVE_SECRETA.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] bytesDesencriptados = Base64.getDecoder().decode(datosEncriptados);
            return new String(cipher.doFinal(bytesDesencriptados), "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }
}