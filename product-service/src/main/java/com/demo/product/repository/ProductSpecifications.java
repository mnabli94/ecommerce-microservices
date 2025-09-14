package com.demo.product.repository;

import com.demo.product.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecifications {
    public static Specification<Product> nameContains(String name) {
        return (root, query, cb) ->
                (name == null || name.isBlank())
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Product> categoryIdEquals(Long categoryId) {
        return (root, query, cb) ->
                categoryId == null
                        ? cb.conjunction()
                        : cb.equal(root.get("category").get("id"), categoryId);

    }

    public static Specification<Product> minPrice(BigDecimal min) {
        return (root, query, cb) ->
                min == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> maxPrice(BigDecimal max) {
        return (root, query, cb) ->
                (max == null)
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(root.get("price"), max);
    }
}
