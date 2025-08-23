package com.demo.product.service;

import com.demo.product.mapper.CategoryMapper;
import com.demo.product.model.Category;
import com.demo.product.repository.CategoryRepository;
import com.demo.product.dto.CategoryDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public CategoryDTO create(CategoryDTO in) {
        return CategoryMapper.toDto(repo.save(new Category(in.name())));
    }

    public CategoryDTO find(Long id) {
        var category = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Category with id %d not found".formatted(id)));
        return CategoryMapper.toDto(category);
    }

    public Page<CategoryDTO> list(Pageable pageable) {
        return repo.findAll(pageable).map(CategoryMapper::toDto);
    }

    @Transactional
    public CategoryDTO update(Long id, CategoryDTO in) {
        var category = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Category %d introuvable".formatted(id)));
        category.setName(in.name());
        return CategoryMapper.toDto(repo.save(category));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id))  {
            throw new EntityNotFoundException("Category %d introuvable".formatted(id));
        }
        repo.deleteById(id);
    }
}