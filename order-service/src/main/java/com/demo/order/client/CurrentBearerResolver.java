package com.demo.order.client;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentBearerResolver {
    public Optional<String> resolveBearer() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jat) {
            return Optional.of("Bearer " + jat.getToken().getTokenValue());
        }
        return Optional.empty();
    }
}
