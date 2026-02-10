package com.omar.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@SpringBootApplication
public class ECommerceApplication {

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(ECommerceApplication.class, args);
	}

	private static void loadDotEnv() {
		Path envPath = Path.of(".env");
		if (!Files.exists(envPath)) {
			return;
		}

		try (Stream<String> lines = Files.lines(envPath)) {
			lines.map(String::trim)
				.filter(line -> !line.isEmpty())
				.filter(line -> !line.startsWith("#"))
				.forEach(ECommerceApplication::setPropertyIfMissing);
		} catch (IOException e) {
			System.err.println("Failed to read .env file: " + e.getMessage());
		}
	}

	private static void setPropertyIfMissing(String line) {
		int equalsIndex = line.indexOf('=');
		if (equalsIndex <= 0) {
			return;
		}
		String key = line.substring(0, equalsIndex).trim();
		String value = line.substring(equalsIndex + 1).trim();
		if (!key.isEmpty() && System.getProperty(key) == null && System.getenv(key) == null) {
			System.setProperty(key, value);
		}
	}
}
