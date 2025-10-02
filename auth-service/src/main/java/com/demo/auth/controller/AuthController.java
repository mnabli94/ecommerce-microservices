package com.demo.auth.controller;

import com.demo.auth.security.TokenService;
import com.nimbusds.jose.JOSEException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final TokenService tokens;

    public AuthController(TokenService tokens) {
        this.tokens = tokens;
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> token(@RequestParam String username,
                                                     @RequestParam String password) throws JOSEException {

        if (!"user".equals(username) || !"pass".equals(password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String access = tokens.generateAccessToken(username, List.of("ROLE_USER"));

        return ResponseEntity.ok(Map.of(
                "access_token", access,
                "token_type", "Bearer",
                "expires_in", 300
        ));
    }
}
