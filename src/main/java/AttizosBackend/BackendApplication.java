package AttizosBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.File;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		// 1. Buscamos la ruta de APPDATA exacta
		String rutaAppData = System.getenv("APPDATA") + File.separator + "Attizos" + File.separator +"backend"+File.separator+ "application.properties";
		File archivoConfig = new File(rutaAppData);
		if (!archivoConfig.exists()) {
			System.err.println("======================================================");
			System.err.println("❌ ERROR CRÍTICO: FALTAN CREDENCIALES DE BASE DE DATOS");
			System.err.println("No se encontró el archivo en: " + rutaAppData);
			System.err.println("Por favor, crea ese archivo y ponle los datos de PostgreSQL.");
			System.err.println("======================================================");
			System.exit(1); // Apagamos antes de que Spring Boot explote
		}

		// 3. Le decimos a Spring Boot que use ESE archivo obligatoriamente (sin el optional)
		System.setProperty("spring.config.additional-location", "file:" + rutaAppData);
		System.out.println("🚀 Cargando configuración externa desde: " + rutaAppData);

		// 4. Encendemos Spring Boot
		SpringApplication.run(BackendApplication.class, args);
	}
}