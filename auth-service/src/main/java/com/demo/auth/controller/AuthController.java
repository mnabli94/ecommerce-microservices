package com.demo.auth.controller;

import com.demo.auth.dto.*;
import com.demo.auth.security.KeyProvider;
import com.demo.auth.service.AuthService;
import com.demo.auth.service.RefreshService;
import com.demo.auth.service.TokenService;
import com.demo.auth.service.UserService;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(UserService userService, TokenService tokenService, RefreshService refreshService, AuthService authService, KeyProvider keys) {
        this.authService = authService;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> token(@RequestBody @Valid LoginRequest loginRequest) throws JOSEException {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid RefreshRequest refreshRequest) throws Exception {
        return ResponseEntity.ok(authService.refresh(refreshRequest));
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revoke(@RequestBody @Valid RevokeRequest revokeRequest) {
        authService.revokeCascade(revokeRequest);
        return ResponseEntity.noContent().build();
    }
}
