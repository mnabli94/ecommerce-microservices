package com.demo.order.service;

import com.demo.order.client.ProductClient;
import com.demo.order.dto.out.ProductDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

@Component
public class ProductCaller {
    private final ProductClient productClient;

    public ProductCaller(ProductClient productClient) {
        this.productClient = productClient;
    }
    @CircuitBreaker(name = "product-service")
    @Retry(name = "product-service")
    public ProductDTO getProduct(Long productId) {
        return productClient.getProduct(productId);
    }

}
