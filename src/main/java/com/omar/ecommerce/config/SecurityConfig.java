package com.omar.ecommerce.config;

import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.services.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final CustomUserDetailsService customUserDetailsService;

    @Value("${app.remember-me.key:change-this-key}")
    private String rememberMeKey;

    // CUSTOM SUCCESS HANDLER - User vs Admin Detection
    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            RequestCache requestCache = new HttpSessionRequestCache();
            SavedRequest savedRequest = requestCache.getRequest(request, response);
            if (savedRequest != null) {
                requestCache.removeRequest(request, response);
                String safeRedirect = resolveSafeRedirectUrl(request, savedRequest.getRedirectUrl());
                response.sendRedirect(safeRedirect);
                return;
            }

            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if ("ADMIN".equals(user.getRole().name())) {
                    response.sendRedirect("/admin/products");
                } else {
                    response.sendRedirect("/products");
                }
            } else {
                response.sendRedirect("/products");
            }
        };
    }

    private String resolveSafeRedirectUrl(HttpServletRequest request, String candidateUrl) {
        if (candidateUrl == null || candidateUrl.isBlank()) {
            return "/products";
        }

        try {
            URI uri = URI.create(candidateUrl);

            if (uri.isAbsolute()) {
                String scheme = uri.getScheme();
                String host = uri.getHost();
                int uriPort = uri.getPort() == -1
                        ? ("https".equalsIgnoreCase(scheme) ? 443 : 80)
                        : uri.getPort();
                int requestPort = request.getServerPort();

                boolean sameOrigin =
                        request.getScheme().equalsIgnoreCase(scheme) &&
                        request.getServerName().equalsIgnoreCase(host) &&
                        requestPort == uriPort;

                if (!sameOrigin) {
                    return "/products";
                }

                String path = uri.getRawPath();
                if (path == null || path.isBlank() || !path.startsWith("/") || path.startsWith("//")) {
                    return "/products";
                }

                String query = uri.getRawQuery();
                return query == null ? path : path + "?" + query;
            }

            String path = uri.getRawPath();
            if (path == null || path.isBlank() || !path.startsWith("/") || path.startsWith("//")) {
                return "/products";
            }

            String query = uri.getRawQuery();
            return query == null ? path : path + "?" + query;
        } catch (IllegalArgumentException ex) {
            return "/products";
        }
    }

    // Authentication Provider (Spring Security 6.x)
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Security filter chain - MAIN CONFIG
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/verify-email",
                                "/resend-verification",
                                "/forgot-password",
                                "/reset-password",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/products**",
                                "/products/**",
                                "/contact",
                                "/contact.html",
                                "/contact/**",
                                "/about",
                                "/about.html",
                                "/aboutUs",
                                "/aboutUs.html",
                                "/privacy",
                                "/privacy/",
                                "/privacy.html",
                                "/terms",
                                "/terms/",
                                "/terms.html",
                                "/privacy/**",
                                "/terms/**",
                                "/support",
                                "/support/",
                                "/support.html",
                                "/support/**",
                                "/wishlists/shared/**",
                                "/products/details",
                                "/products/details/",
                                "/products/details/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/cart/**", "/orders/**", "/order-summary").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(successHandler())
                        .failureHandler((request, response, exception) -> response.sendRedirect("/login?error"))
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .rememberMeParameter("remember-me")
                        .key(rememberMeKey)
                        .userDetailsService(customUserDetailsService)
                        .tokenValiditySeconds(60 * 60 * 24 * 14)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    // Password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // UserDetailsService bean
    @Bean
    public UserDetailsService userDetailsService() {
        return customUserDetailsService;
    }
}
