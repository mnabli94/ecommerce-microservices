package com.demo.order.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    /**
     * Disable Feign's built-in retry to avoid duplication with Resilience4j.
     * Retry logic is handled by {@link ResilienceConfig#productServiceRetryCustomizer()}.
     */
    @Bean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }
}
