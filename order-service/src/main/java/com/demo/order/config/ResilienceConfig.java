package com.demo.order.config;

import feign.FeignException;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.core.IntervalFunction;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Set;

@Configuration
public class ResilienceConfig {

    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(503, 504, 429);

    @Bean
    public RetryConfigCustomizer productServiceRetryCustomizer() {
        return RetryConfigCustomizer.of("product-service", builder -> builder.maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(200), 2.0))
                .retryOnException(e -> e instanceof FeignException fe
                        && RETRYABLE_STATUSES.contains(fe.status())));
    }

    @Bean
    public CircuitBreakerConfigCustomizer productServiceCbCustomizer() {
        return CircuitBreakerConfigCustomizer.of("product-service", builder -> builder.slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true));
    }
}
