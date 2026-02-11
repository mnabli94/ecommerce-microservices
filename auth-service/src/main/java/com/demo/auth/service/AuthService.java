package com.demo.auth.service;

import com.demo.auth.config.MetricsConfig;
import com.demo.auth.dto.*;
import com.demo.auth.entity.RefreshToken;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {
    private final UserService userService;
    private final TokenService tokenService;
    private final RefreshService refreshService;
    private final MetricsConfig.AuthMetrics metrics;
    private final String tokenType;
    private final long accessTtl;

    public AuthService(UserService userService,
            TokenService tokenService,
            RefreshService refreshService,
            MetricsConfig.AuthMetrics metrics,
            @Value("${auth.token-type:Bearer}") String tokenType,
            @Value("${auth.access-ttl-seconds}") long accessTtl) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.refreshService = refreshService;
        this.metrics = metrics;
        this.tokenType = tokenType;
        this.accessTtl = accessTtl;
    }

    public TokenResponse login(LoginRequest request) throws Exception {
        return metrics.getLoginTimer().recordCallable(() -> {
            log.info("Login attempt for user: {}", request.username());
            try {
                var user = userService.authenticate(request.username(), request.password());
                var session = userService.prepareSession(user.getUsername());

                var accessToken = tokenService.createAccessToken(
                        session.username(),
                        session.roles(),
                        session.scopes());
                var refreshToken = refreshService.issue(user, "client-web");

                metrics.getLoginSuccessCounter().increment();
                log.info("Login successful for user: {}", request.username());
                return new TokenResponse(accessToken, refreshToken.getId(), tokenType, accessTtl);

            } catch (Exception e) {
                metrics.getLoginFailureCounter().increment();
                if (e.getMessage() != null && e.getMessage().contains("locked")) {
                    metrics.getLoginBruteForceCounter().increment();
                }
                throw e;
            }
        });
    }

    public TokenResponse refresh(RefreshRequest request) {
        log.debug("Token refresh requested");
        RefreshToken next = refreshService.rotate(request.refreshToken());
        var session = userService.prepareSession(next.getUser().getUsername());

        String access = tokenService.createAccessToken(
                session.username(),
                session.roles(),
                session.scopes());

        metrics.getTokenRefreshCounter().increment();
        log.info("Token refreshed for user: {}", session.username());
        return new TokenResponse(access, next.getId(), tokenType, accessTtl);
    }

    public void revokeToken(@Valid RevokeRequest revokeRequest) {
        log.info("Revoking specific refresh token: {}", revokeRequest.refreshToken());
        refreshService.revokeToken(revokeRequest.refreshToken());
        metrics.getTokenRevokeCounter().increment();
        log.debug("Refresh token revoked successfully");
    }

    public void logoutEverywhere(String username) {
        log.info("Logging out user from all devices: {}", username);
        var user = userService.findByUsername(username);
        refreshService.revokeAllByUser(user);
    }
}