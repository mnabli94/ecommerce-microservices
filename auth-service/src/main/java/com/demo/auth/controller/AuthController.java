package com.demo.auth.controller;

import com.demo.auth.dto.*;
import com.demo.auth.service.AuthService;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login user", description = "Authenticates a user and returns an access token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated", content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials")
    })
    @PostMapping("/token")
    public ResponseEntity<TokenResponse> token(@RequestBody @Valid LoginRequest loginRequest) throws JOSEException {
        log.debug("Logging message within token()");
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @Operation(summary = "Refresh token", description = "Refreshes an expired access token using a refresh token")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid RefreshRequest refreshRequest) throws Exception {
        return ResponseEntity.ok(authService.refresh(refreshRequest));
    }

    @Operation(summary = "Revoke token", description = "Revokes a refresh token")
    @ApiResponse(responseCode = "204", description = "Token revoked successfully")
    @PostMapping("/revoke")
    public ResponseEntity<Void> revoke(@RequestBody @Valid RevokeRequest revokeRequest) {
        authService.revokeCascade(revokeRequest);
        return ResponseEntity.noContent().build();
    }
}
