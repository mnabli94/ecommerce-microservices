package com.demo.product.mapper;

import com.demo.product.dto.CategoryDTO;
import com.demo.product.entity.Category;

import java.util.Objects;

public final class CategoryMapper {

    public static CategoryDTO toDto(Category entity) {
        if (entity == null) return null;
        return new CategoryDTO(entity.getId(), entity.getName());
    }

    public static Category toNewEntity(CategoryDTO dto) {
        Objects.requireNonNull(dto, "dto is required");
        return new Category(null, trim(dto.name()));
    }

    public static void applyPut(Category target, CategoryDTO dto) {
        Objects.requireNonNull(target, "target entity is required");
        Objects.requireNonNull(dto, "dto is required");
        target.setName(trim(dto.name()));
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
