package com.omar.ecommerce.repositories;

import com.omar.ecommerce.entities.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    boolean existsByToken(String token);

    void deleteByUserId(Long userId);
}
