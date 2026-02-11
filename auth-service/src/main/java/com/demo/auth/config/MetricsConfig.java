package com.demo.auth.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public AuthMetrics authMetrics(MeterRegistry registry) {
        return new AuthMetrics(registry);
    }

    public static class AuthMetrics {
        private final Counter loginSuccessCounter;
        private final Counter loginFailureCounter;
        private final Counter loginBruteForceCounter;
        private final Counter tokenRefreshCounter;
        private final Counter tokenRevokeCounter;
        private final Counter userCreationCounter;
        private final Timer loginTimer;
        private final Timer tokenValidationTimer;

        public AuthMetrics(MeterRegistry registry) {
            this.loginSuccessCounter = Counter.builder("auth.login.success")
                    .description("Number of successful login attempts")
                    .register(registry);

            this.loginFailureCounter = Counter.builder("auth.login.failure")
                    .description("Number of failed login attempts")
                    .register(registry);

            this.loginBruteForceCounter = Counter.builder("auth.login.brute_force")
                    .description("Number of potential brute force attempts detected")
                    .register(registry);

            this.tokenRefreshCounter = Counter.builder("auth.token.refresh")
                    .description("Number of token refresh operations")
                    .register(registry);

            this.tokenRevokeCounter = Counter.builder("auth.token.revoke")
                    .description("Number of token revoke operations")
                    .register(registry);

            this.userCreationCounter = Counter.builder("auth.user.creation")
                    .description("Number of user creation operations")
                    .register(registry);

            this.loginTimer = Timer.builder("auth.login.duration")
                    .description("Time taken for login operations")
                    .register(registry);

            this.tokenValidationTimer = Timer.builder("auth.token.validation.duration")
                    .description("Time taken for token validation operations")
                    .register(registry);
        }

        public Counter getLoginSuccessCounter() { return loginSuccessCounter; }
        public Counter getLoginFailureCounter() { return loginFailureCounter; }
        public Counter getLoginBruteForceCounter() { return loginBruteForceCounter; }
        public Counter getTokenRefreshCounter() { return tokenRefreshCounter; }
        public Counter getTokenRevokeCounter() { return tokenRevokeCounter; }
        public Counter getUserCreationCounter() { return userCreationCounter; }
        public Timer getLoginTimer() { return loginTimer; }
        public Timer getTokenValidationTimer() { return tokenValidationTimer; }
    }
}