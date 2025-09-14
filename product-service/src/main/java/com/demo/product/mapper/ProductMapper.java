package com.demo.product.mapper;

import com.demo.product.dto.ProductDTO;
import com.demo.product.entity.Category;
import com.demo.product.entity.Product;

import java.util.Objects;

public final class ProductMapper {

    public static ProductDTO toDto(Product entity) {
        if (entity == null) return null;
        return new ProductDTO(
                entity.getId(),
                entity.getName(),
                entity.getPrice(),
                entity.isInStock(),
                entity.getCategory() != null ? entity.getCategory().getId() : null
        );
    }
    
    public static Product toNewEntity(ProductDTO dto, Category category) {
        Objects.requireNonNull(dto, "dto is required");
        // category peut être null si produit sans catégorie (selon règles métier)
        return new Product(
                null,
                trim(dto.name()),
                dto.price(),
                dto.inStock(),
                category
        );
    }
    
    public static void applyPut(Product target, ProductDTO dto, Category category) {
        Objects.requireNonNull(target, "target entity is required");
        Objects.requireNonNull(dto, "dto is required");
        target.setName(trim(dto.name()));
        target.setPrice(dto.price());
        target.setInStock(dto.inStock());
        target.setCategory(category);
    }
    
    private static String trim(String s) {return s == null ? null : s.trim();}

}
