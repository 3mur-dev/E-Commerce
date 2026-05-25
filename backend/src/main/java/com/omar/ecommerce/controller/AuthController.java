package com.omar.ecommerce.controller;

import com.omar.ecommerce.dtos.*;
import com.omar.ecommerce.dtos.AuthInfoResponse;
import com.omar.ecommerce.entities.Role;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.entities.UserStatus;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.security.JwtService;
import com.omar.ecommerce.services.AuthService;
import com.omar.ecommerce.services.CustomUserDetailsService;
import com.omar.ecommerce.services.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final EmailService emailService;

    @GetMapping
    public ApiResponse<AuthInfoResponse> info() {
        return ApiResponse.<AuthInfoResponse>builder()
                .success(true)
                .message("Authentication endpoints")
                .data(new AuthInfoResponse(
                        "POST /api/auth/login",
                        "POST /api/auth/register",
                        "GET /api/auth/me"
                ))
                .build();
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        return ResponseEntity.ok(ApiResponse.<CurrentUserResponse>builder()
                .success(true)
                .message("Current user retrieved successfully")
                .data(new CurrentUserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole()
                ))
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        if (user.isDeleted()) {
            throw new DisabledException("Account is disabled");
        }

        if (!user.isEmailVerified()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(false)
                            .message("Please verify your email")
                            .data(null)
                            .build());
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String token = jwtService.generateToken(userDetails);

        AuthResponse authResponse = new AuthResponse(
                token,
                "Bearer",
                userDetails.getUsername()
        );

        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .success(true)
                        .message("Login successful")
                        .data(authResponse)
                        .build()
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {

        authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Check your email to verify account")
                        .data(null)
                        .build());
    }


    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Boolean>> verifyEmail(@RequestParam String token) {

        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .success(true)
                .message("Email verified successfully")
                .data(true)
                .build());
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resend(@RequestParam String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                String token = UUID.randomUUID().toString();
                user.setVerificationToken(token);
                user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
                userRepository.save(user);
                emailService.sendVerificationEmail(user.getEmail(), token);
            }
        });

        return ResponseEntity.accepted().body(ApiResponse.<Void>builder()
                .success(true)
                .message("If the account exists, a verification email will be sent")
                .data(null)
                .build());
    }
}
