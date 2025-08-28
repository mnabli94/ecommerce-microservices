package com.demo.product.security;

import com.demo.product.security.dto.LoginRequest;
import com.demo.product.security.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwt;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest req) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
            String token = jwt.generate(auth.getName());
            return ResponseEntity.ok(new TokenResponse(token));
        } catch (BadCredentialsException | UsernameNotFoundException e) {
            return ResponseEntity.status(401).build();
        }
    }
}
