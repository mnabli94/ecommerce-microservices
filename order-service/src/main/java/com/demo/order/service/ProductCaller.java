package com.demo.order.service;

import com.demo.order.client.CurrentBearerResolver;
import com.demo.order.client.ProductClient;
import com.demo.order.dto.out.ProductDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

@Component
public class ProductCaller {
    private final ProductClient productClient;
    private final CurrentBearerResolver bearerResolver;
    public ProductCaller(ProductClient productClient, CurrentBearerResolver bearerResolver) {
        this.productClient = productClient;
        this.bearerResolver = bearerResolver;
    }
    @CircuitBreaker(name = "product-service")
    @Retry(name = "product-service")
    public ProductDTO getProduct(Long productId) {
        String bearer = bearerResolver.resolveBearer()
                .orElseThrow(() -> new IllegalStateException("No JWT in SecurityContext"));
        return productClient.getProduct(productId, bearer);
    }

}
