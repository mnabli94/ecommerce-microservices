package com.demo.order.config;

import feign.Request;
import feign.RequestInterceptor;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
public class FeignConfig {

    @PostConstruct
    void inherit() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public RequestInterceptor bearerFromSecurityContext() {
        return template -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jat) {
                String token = jat.getToken().getTokenValue();
                if (token != null) {
                    token = token.startsWith("Bearer ") ? token.substring(7).trim() : token;
                    template.header("Authorization", "Bearer " + token);
                }
            }
        };
    }

    @Bean
    public Retryer feignRetryer() {
        // period (ms), maxPeriod (ms), maxAttempts (incluant le 1er appel)
        return new Retryer.Default(100, 500, 3); // => 1 appel + 2 retries
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        ErrorDecoder defaultDecoder = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            int s = response.status();
            // rends 503/504/429 "retryables"
            if (s == 503 || s == 504 || s == 429) {
                Request req = response.request();
                return new RetryableException(
                        s,
                        "retryable-status-" + s,
                        req == null ? null : req.httpMethod(),
                        (Throwable) null, // cause
                        (Long) null, // retryAfter
                        req
                );
            }
            return defaultDecoder.decode(methodKey, response);
        };
    }

}