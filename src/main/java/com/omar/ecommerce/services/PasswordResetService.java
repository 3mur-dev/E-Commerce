package com.omar.ecommerce.services;

import com.omar.ecommerce.entities.PasswordResetToken;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.PasswordResetTokenRepository;
import com.omar.ecommerce.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int CODE_LENGTH = 6;
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.reset.token-minutes:60}")
    private int tokenMinutes;

    @Transactional
    public Optional<String> createResetToken(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        tokenRepository.deleteByUserId(user.getId());

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(generateCode());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(tokenMinutes));
        tokenRepository.save(token);

        return Optional.of(token.getToken());
    }

    public Optional<PasswordResetToken> validateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(rawToken.trim());
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        PasswordResetToken token = tokenOpt.get();
        if (token.getUsedAt() != null) {
            return Optional.empty();
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        return Optional.of(token);
    }

    @Transactional
    public boolean resetPasswordWithCode(String email, String rawToken, String newPassword) {
        if (email == null || email.isBlank()) {
            return false;
        }
        Optional<PasswordResetToken> tokenOpt = validateToken(rawToken);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }

        PasswordResetToken token = tokenOpt.get();
        User user = token.getUser();
        if (!user.getEmail().equalsIgnoreCase(email.trim())) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
        return true;
    }

    private String generateCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            int code = SECURE_RANDOM.nextInt(1_000_000);
            String candidate = String.format("%0" + CODE_LENGTH + "d", code);
            if (!tokenRepository.existsByToken(candidate)) {
                return candidate;
            }
        }
        int fallback = SECURE_RANDOM.nextInt(900000) + 100000;
        return String.valueOf(fallback);
    }
}
