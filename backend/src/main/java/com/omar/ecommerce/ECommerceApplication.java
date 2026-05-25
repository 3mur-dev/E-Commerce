package com.omar.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@EnableAsync
@EnableCaching
@SpringBootApplication
public class ECommerceApplication {

	public static void main(String[] args) {
		applyLocalNetworkDefaults();
		loadDotEnv();
		applyDatabaseUrlFallback();
		SpringApplication.run(ECommerceApplication.class, args);
	}

	private static void applyLocalNetworkDefaults() {
		if (!isLocalRun()) {
			return;
		}

		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.net.preferIPv6Addresses", "false");
	}

	private static void loadDotEnv() {
		Path envPath = locateDotEnv();
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

	private static Path locateDotEnv() {
		Path current = Path.of("").toAbsolutePath().normalize();
		Path found = findDotEnvInParents(current);
		if (found != null) {
			return found;
		}

		try {
			Path codeSource = Path.of(
					ECommerceApplication.class.getProtectionDomain()
							.getCodeSource()
							.getLocation()
							.toURI()
			).toAbsolutePath().normalize();
			Path start = Files.isDirectory(codeSource) ? codeSource : codeSource.getParent();
			found = findDotEnvInParents(start);
			if (found != null) {
				return found;
			}
		} catch (URISyntaxException | SecurityException e) {
			System.err.println("Unable to resolve application location for .env lookup: " + e.getMessage());
		}

		return current.resolve(".env");
	}

	private static Path findDotEnvInParents(Path start) {
		Path dir = start;
		while (dir != null) {
			Path candidate = dir.resolve(".env");
			if (Files.exists(candidate)) {
				return candidate;
			}
			dir = dir.getParent();
		}
		return null;
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
		String configuredUrl = firstNonBlank(
				System.getProperty("DB_URL"),
				System.getenv("DB_URL")
		);
		if (configuredUrl != null && !configuredUrl.isBlank()) {
			normalizeAndApplyDatabaseUrl(configuredUrl);
			return;
		}

		String databaseUrl = firstNonBlank(
				System.getProperty("DATABASE_URL"),
				System.getenv("DATABASE_URL")
		);
		if (databaseUrl == null || databaseUrl.isBlank()) {
			return;
		}

		normalizeAndApplyDatabaseUrl(databaseUrl);
	}

	private static void normalizeAndApplyDatabaseUrl(String databaseUrl) {
		if (databaseUrl.startsWith("jdbc:")) {
			System.setProperty("DB_URL", normalizeJdbcUrl(databaseUrl));
			return;
		}

		try {
			URI uri = new URI(databaseUrl);
			String scheme = uri.getScheme();
			if (scheme == null || (!"postgresql".equalsIgnoreCase(scheme) && !"postgres".equalsIgnoreCase(scheme))) {
				System.err.println("Unsupported database URL scheme: " + scheme);
				return;
			}

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

			System.setProperty("DB_URL", normalizeJdbcUrl(buildJdbcUrl(host, port, path, query)));
			if (username != null && !hasValue("DB_USERNAME")) {
				System.setProperty("DB_USERNAME", username);
			}
			if (password != null && !hasValue("DB_PASSWORD")) {
				System.setProperty("DB_PASSWORD", password);
			}
		} catch (Exception e) {
			System.err.println("Failed to parse database URL: " + e.getMessage());
		}
	}

	private static String buildJdbcUrl(String host, int port, String path, String query) {
		StringBuilder jdbc = new StringBuilder("jdbc:postgresql://");
		jdbc.append(host);
		if (port > 0) {
			jdbc.append(":").append(port);
		}
		if (path != null) {
			jdbc.append(path);
		}
		if (query == null || query.isBlank()) {
			jdbc.append("?sslmode=require&channel_binding=require&connectTimeout=10");
		} else {
			jdbc.append("?").append(query);
			if (!query.contains("sslmode=")) {
				jdbc.append("&sslmode=require");
			}
			if (!query.contains("channel_binding=")) {
				jdbc.append("&channel_binding=require");
			}
			if (!query.contains("connectTimeout=")) {
				jdbc.append("&connectTimeout=10");
			}
		}
		return jdbc.toString();
	}

	private static String normalizeJdbcUrl(String jdbcUrl) {
		if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql:")) {
			return jdbcUrl;
		}

		String base = jdbcUrl;
		String query = "";
		int questionMark = jdbcUrl.indexOf('?');
		if (questionMark >= 0) {
			base = jdbcUrl.substring(0, questionMark);
			query = jdbcUrl.substring(questionMark + 1);
		}

		StringBuilder normalized = new StringBuilder(base);
		if (query.isBlank()) {
			normalized.append("?sslmode=require&channel_binding=require&connectTimeout=10");
			return normalized.toString();
		}

		normalized.append("?").append(query);
		if (!query.contains("sslmode=")) {
			normalized.append("&sslmode=require");
		}
		if (!query.contains("channel_binding=")) {
			normalized.append("&channel_binding=require");
		}
		if (!query.contains("connectTimeout=")) {
			normalized.append("&connectTimeout=10");
		}

		String result = normalized.toString();
		if (isLocalRun()) {
			result = preferDirectNeonHost(result);
		}
		return result;
	}

	private static String preferDirectNeonHost(String jdbcUrl) {
		if (!jdbcUrl.contains("-pooler.")) {
			return jdbcUrl;
		}

		int hostStart = "jdbc:postgresql://".length();
		int pathStart = jdbcUrl.indexOf('/', hostStart);
		if (pathStart < 0) {
			return jdbcUrl.replace("-pooler.", ".");
		}

		String prefix = jdbcUrl.substring(0, pathStart).replace("-pooler.", ".");
		String suffix = jdbcUrl.substring(pathStart);
		int queryStart = suffix.indexOf('?');
		if (queryStart < 0) {
			return prefix + suffix;
		}

		String path = suffix.substring(0, queryStart);
		String query = suffix.substring(queryStart + 1);
		StringBuilder directQuery = new StringBuilder();
		for (String part : query.split("&")) {
			if (part.isBlank() || part.startsWith("channel_binding=")) {
				continue;
			}
			if (!directQuery.isEmpty()) {
				directQuery.append('&');
			}
			directQuery.append(part);
		}
		return prefix + path + (directQuery.isEmpty() ? "" : "?" + directQuery);
	}

	private static boolean isLocalRun() {
		return System.getenv("RENDER") == null && System.getenv("PORT") == null;
	}

	private static boolean hasValue(String key) {
		return firstNonBlank(System.getProperty(key), System.getenv(key)) != null;
	}

	private static String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first;
		}
		if (second != null && !second.isBlank()) {
			return second;
		}
		return null;
	}
}
