package com.demo.order.client;

import com.demo.order.config.FeignConfig;
import com.demo.order.dto.out.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "product-service", url = "${product-service.url}",
        fallbackFactory = ProductClientFallbackFactory.class, configuration = FeignConfig.class)
public interface ProductClient {

    //@Cacheable(value = "products", key = "#id")
    @GetMapping("/api/products/{id}")
    ProductDTO getProduct(@PathVariable("id") Long id, @RequestHeader("Authorization") String authorization);
}
