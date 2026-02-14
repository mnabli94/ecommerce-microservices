package com.demo.order.client;

import com.demo.order.dto.out.ProductDTO;
import com.demo.order.exception.ProductServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    private static final Logger logger = LoggerFactory.getLogger(ProductClientFallbackFactory.class);
    private final MeterRegistry meterRegistry;

    public ProductClientFallbackFactory(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ProductClient create(Throwable cause) {
        return new ProductClient() {
            @Override
            public ProductDTO getProduct(Long id, String bearer) {
                String causeType;
                if (cause instanceof CallNotPermittedException) {
                    logger.warn("Circuit breaker is OPEN for product-service: {}", cause.getMessage());
                    causeType = "circuit_breaker_open";
                } else if (cause instanceof FeignException) {
                    logger.warn("Feign error calling product-service: {}", cause.getMessage());
                    causeType = "feign_error";
                } else {
                    logger.warn("Unexpected error calling product-service: {}", cause.getMessage());
                    causeType = "unknown_error";
                }

                meterRegistry.counter("product.fallback.invoked",
                        "service", "order-service",
                        "cause", causeType,
                        "product_id", id.toString()
                ).increment();

                throw new ProductServiceUnavailableException(
                        "Product service unavailable for product ID %d: %s".formatted(id, causeType), cause);
            }
        };
    }
}
