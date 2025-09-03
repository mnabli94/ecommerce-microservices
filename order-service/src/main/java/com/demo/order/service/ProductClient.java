package com.demo.order.service;

import com.demo.order.dto.out.ProductDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "${product-service.url}")
public interface ProductClient {

    @Cacheable(value = "products", key = "#id")
    @GetMapping("products/{id}")
    ProductDTO getProduct(@PathVariable("id") Long id);
}
