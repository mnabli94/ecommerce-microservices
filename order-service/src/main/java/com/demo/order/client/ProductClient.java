package com.demo.order.client;

import com.demo.order.config.FeignConfig;
import com.demo.order.dto.out.ProductDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "${product-service.url}",
        fallbackFactory = ProductClientFallbackFactory.class, configuration = FeignConfig.class)
public interface ProductClient {

    //@Cacheable(value = "products", key = "#id")
    @GetMapping("/api/products/{id}")
    //@CircuitBreaker(name = "productService")
    //@Retry(name = "product-service")
    ProductDTO getProduct(@PathVariable("id") Long id);
}
