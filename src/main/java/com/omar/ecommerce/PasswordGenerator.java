package com.omar.ecommerce;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashed = encoder.encode("omor1234"); // desired password
        System.out.println("BCrypt hash: " + hashed);

        boolean match = encoder.matches("omor1234", hashed); // ✅ correct
        System.out.println(match);
    }
}

