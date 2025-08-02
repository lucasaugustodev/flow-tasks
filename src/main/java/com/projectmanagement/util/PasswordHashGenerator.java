package com.projectmanagement.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "password123";
        String hashedPassword = encoder.encode(password);
        System.out.println("Password: " + password);
        System.out.println("Hashed: " + hashedPassword);
        
        // Test if it matches
        boolean matches = encoder.matches(password, hashedPassword);
        System.out.println("Matches: " + matches);
    }
}
