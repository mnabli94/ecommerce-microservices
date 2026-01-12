package com.demo.product.service;

import com.demo.product.mapper.CategoryMapper;
import com.demo.product.entity.Category;
import com.demo.product.repository.CategoryRepository;
import com.demo.product.dto.CategoryDTO;
import com.demo.product.repository.CategorySpecifications;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CategoryService {

    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public CategoryDTO create(CategoryDTO in) {
        log.info("Creating category: name={}", in.name());
        CategoryDTO saved = CategoryMapper.toDto(repo.save(new Category(in.name())));
        log.info("Category created: id={}", saved.id());
        return saved;
    }

    public CategoryDTO find(Long id) {
        log.debug("Finding category by id: {}", id);
        var category = repo.findById(id).orElseThrow(() -> {
            log.error("Category not found: id={}", id);
            return new EntityNotFoundException("Category with id %d not found".formatted(id));
        });
        return CategoryMapper.toDto(category);
    }

    public Page<CategoryDTO> search(Long id, String name, Pageable pageable) {
        Specification<Category> spec = Specification
                .where(CategorySpecifications.nameContains(name))
                .and(CategorySpecifications.idEquals(id));
        return repo.findAll(spec, pageable).map(CategoryMapper::toDto);
    }

    @Transactional
    public CategoryDTO update(Long id, CategoryDTO in) {
        log.info("Updating category: id={}", id);
        var category = repo.findById(id).orElseThrow(() -> {
            log.error("Category not found for update: id={}", id);
            return new EntityNotFoundException("Category %d introuvable".formatted(id));
        });
        category.setName(in.name());
        log.info("Category updated: id={}", id);
        return CategoryMapper.toDto(repo.save(category));
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting category: id={}", id);
        if (!repo.existsById(id)) {
            log.error("Category not found for deletion: id={}", id);
            throw new EntityNotFoundException("Category %d not found".formatted(id));
        }
        repo.deleteById(id);
        log.info("Category deleted: id={}", id);
    }
}