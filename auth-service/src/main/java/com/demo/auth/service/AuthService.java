package com.demo.auth.service;

import com.demo.auth.dto.LoginRequest;
import com.demo.auth.dto.RefreshRequest;
import com.demo.auth.dto.RevokeRequest;
import com.demo.auth.dto.TokenResponse;
import com.demo.auth.entity.RefreshToken;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {
    private final UserService userService;
    private final TokenService tokenService;
    private final RefreshService refreshService;

    public AuthService(UserService userService, TokenService tokenService, RefreshService refreshService) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.refreshService = refreshService;
    }

    public TokenResponse login(LoginRequest request) throws JOSEException {
        log.info("Login attempt for user: {}", request.username());
        var user = userService.authenticate(request.username(), request.password());
        log.debug("User authenticated successfully: {}", user.getUsername());

        var accessToken = tokenService.createAccessToken(
                user.getUsername(),
                userService.rolesByUsername(user.getUsername()),
                userService.scopesByUsername(user.getUsername()));
        var refreshToken = refreshService.issue(user.getUsername(), "client-web");
        log.info("Login successful for user: {}", request.username());
        return new TokenResponse(accessToken, refreshToken.getId(), "Bearer", 300);
    }

    public TokenResponse refresh(RefreshRequest request) throws JOSEException {
        log.debug("Token refresh requested");
        RefreshToken next = refreshService.rotate(request.refreshToken());
        String subject = refreshService.getSubject(next.getId());
        String access = tokenService.createAccessToken(subject, userService.rolesByUsername(subject),
                userService.scopesByUsername(subject));
        log.info("Token refreshed for user: {}", subject);
        return new TokenResponse(access, next.getId(), "Bearer", 300);
    }

    public void revokeCascade(@Valid RevokeRequest revokeRequest) {
        log.info("Revoking refresh token cascade");
        refreshService.revokeCascade(revokeRequest.refreshToken());
        log.debug("Refresh token cascade revoked successfully");
    }
}