package com.demo.order.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@RestController
public class AuthController {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostMapping("/api/auth/token")
    public Map<String, String> generateToken(@RequestBody Map<String, String> credentials) {
        // TODO: Valider credentials avec user-service ou DB
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        String token = Jwts.builder()
                .setSubject("test-user")
                .claim("roles", "USER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1h
                .signWith(key)
                .compact();
        return Map.of("token", token);
    }
}