package com.demo.auth.service;

import com.demo.auth.dto.LoginRequest;
import com.demo.auth.dto.RefreshRequest;
import com.demo.auth.dto.RevokeRequest;
import com.demo.auth.dto.TokenResponse;
import com.demo.auth.entity.RefreshToken;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

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
        var user = userService.authenticate(request.username(), request.password());

        var accessToken = tokenService.createAccessToken(
                user.getUsername(),
                userService.rolesByUsername(user.getUsername()),
                userService.scopesByUsername(user.getUsername())
        );
        var refreshToken = refreshService.issue(user.getUsername(), "client-web");
        return new TokenResponse(accessToken, refreshToken.getId(), "Bearer", 300);
    }

    public TokenResponse refresh(RefreshRequest request) throws JOSEException {
        RefreshToken next = refreshService.rotate(request.refreshToken());
        String subject = refreshService.getSubject(next.getId());
        String access = tokenService.createAccessToken(subject, userService.rolesByUsername(subject), userService.scopesByUsername(subject));
        return new TokenResponse(access, next.getId(), "Bearer", 300);
    }

    public void revokeCascade(@Valid RevokeRequest revokeRequest) {
        refreshService.revokeCascade(revokeRequest.refreshToken());
    }
}