package com.omar.ecommerce.config;

import com.omar.ecommerce.entities.Role;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ObjectProvider<PasswordEncoder> passwordEncoderProvider;

    @Value("${app.admin.username:}")
    private String adminUsername;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        PasswordEncoder passwordEncoder = passwordEncoderProvider.getIfAvailable();
        if (passwordEncoder == null) {
            log.info("Admin seeder skipped: PasswordEncoder bean is not available");
            return;
        }

        if (isBlank(adminUsername) || isBlank(adminEmail) || isBlank(adminPassword)) {
            log.info("Admin seeder skipped: app.admin.* properties are not fully configured");
            return;
        }

        String username = adminUsername.trim();
        String email = adminEmail.trim().toLowerCase();

        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            log.info("Admin account already exists for username '{}' or email '{}'", username, email);
            return;
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);

        userRepository.save(admin);
        log.info("Admin account created for username '{}'", username);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
