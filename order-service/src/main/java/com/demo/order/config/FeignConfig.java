package com.demo.order.config;

import feign.Request;
import feign.RequestInterceptor;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor jwtInterceptor() {
        return requestTemplate -> {
            var attrs = RequestContextHolder.getRequestAttributes();
            if(attrs instanceof ServletRequestAttributes servletRequestAttributes) {
                String token = servletRequestAttributes.getRequest().getHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7).trim();
                    requestTemplate.header("Authorization", "Bearer " + token);
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