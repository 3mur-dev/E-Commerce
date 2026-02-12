package com.omar.ecommerce.services;

import com.omar.ecommerce.entities.EmailVerificationToken;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.EmailVerificationTokenRepository;
import com.omar.ecommerce.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Value("${app.verify.token-hours:24}")
    private int tokenHours;

    @Transactional
    public Optional<String> createToken(User user) {
        if (user == null) {
            return Optional.empty();
        }

        tokenRepository.deleteByUserId(user.getId());

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken(generateToken());
        token.setExpiresAt(LocalDateTime.now().plusHours(tokenHours));
        tokenRepository.save(token);

        return Optional.of(token.getToken());
    }

    @Transactional
    public boolean verifyToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(rawToken);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        EmailVerificationToken token = tokenOpt.get();
        if (token.getUsedAt() != null) {
            return false;
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
        return true;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
