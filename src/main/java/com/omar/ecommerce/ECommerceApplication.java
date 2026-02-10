package com.omar.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@SpringBootApplication
public class ECommerceApplication {

	public static void main(String[] args) {
		loadDotEnv();
		applyDatabaseUrlFallback();
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

	private static void applyDatabaseUrlFallback() {
		if (System.getProperty("DB_URL") != null || System.getenv("DB_URL") != null) {
			return;
		}

		String databaseUrl = System.getenv("DATABASE_URL");
		if (databaseUrl == null || databaseUrl.isBlank()) {
			return;
		}

		try {
			URI uri = new URI(databaseUrl);
			String userInfo = uri.getUserInfo();
			String username = null;
			String password = null;
			if (userInfo != null && userInfo.contains(":")) {
				String[] parts = userInfo.split(":", 2);
				username = parts[0];
				password = parts[1];
			}

			String host = uri.getHost();
			int port = uri.getPort();
			String path = uri.getPath();
			String query = uri.getQuery();

			StringBuilder jdbc = new StringBuilder("jdbc:postgresql://");
			jdbc.append(host);
			if (port > 0) {
				jdbc.append(":").append(port);
			}
			if (path != null) {
				jdbc.append(path);
			}
			if (query == null || query.isBlank()) {
				jdbc.append("?sslmode=require");
			} else {
				jdbc.append("?").append(query);
				if (!query.contains("sslmode=")) {
					jdbc.append("&sslmode=require");
				}
			}

			System.setProperty("DB_URL", jdbc.toString());
			if (username != null && System.getProperty("DB_USERNAME") == null && System.getenv("DB_USERNAME") == null) {
				System.setProperty("DB_USERNAME", username);
			}
			if (password != null && System.getProperty("DB_PASSWORD") == null && System.getenv("DB_PASSWORD") == null) {
				System.setProperty("DB_PASSWORD", password);
			}
		} catch (Exception e) {
			System.err.println("Failed to parse DATABASE_URL: " + e.getMessage());
		}
	}
}
