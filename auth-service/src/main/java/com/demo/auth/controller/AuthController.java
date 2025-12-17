package com.demo.auth.controller;

import com.demo.auth.dto.*;
import com.demo.auth.entity.User;
import com.demo.auth.service.RefreshService;
import com.demo.auth.service.TokenService;
import com.demo.auth.service.UserService;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final TokenService tokenService;
    private final RefreshService refreshService;

    public AuthController(UserService userService, TokenService tokenService, RefreshService refreshService) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.refreshService = refreshService;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> token(@RequestBody @Valid LoginRequest request) throws JOSEException {
        var user = userService.authenticate(request.username(), request.password());
        var username = user.getUsername();
        var accessToken = tokenService.createAccessToken(username, userService.rolesByUsername(username), userService.scopesByUsername(username));
        var refreshToken = refreshService.issue(username, "client-web");
        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken.getId(), "Bearer", 300));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid RefreshRequest req) throws Exception {
        var next = refreshService.rotate(req.refreshToken());
        var subject = refreshService.getSubject(next.getId()); // récupère subject du nouveau (ou retourne-le depuis rotate)
        var access = tokenService.createAccessToken(subject, userService.rolesByUsername(subject), userService.scopesByUsername(subject));
        return ResponseEntity.ok(new TokenResponse(access, next.getId(), "Bearer", 300));
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revoke(@RequestBody @Valid RevokeRequest req) {
        refreshService.revokeCascade(req.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
