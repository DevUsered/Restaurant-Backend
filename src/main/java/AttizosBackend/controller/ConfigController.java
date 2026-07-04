package AttizosBackend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${cloudinary.cloud-name}") private String cloudName;
    @Value("${cloudinary.api-key}") private String apiKey;
    @Value("${cloudinary.api-secret}") private String apiSecret;
    @Value("${gemini.api-key}") private String geminiKey;

    @GetMapping("/credenciales")
    public Map<String, String> obtenerCredenciales() {
        Map<String, String> creds = new HashMap<>();
        creds.put("CLOUDINARY_CLOUD_NAME", cloudName);
        creds.put("CLOUDINARY_API_KEY", apiKey);
        creds.put("CLOUDINARY_API_SECRET", apiSecret);
        creds.put("GEMINI_API_KEY", geminiKey);
        return creds;
    }
}