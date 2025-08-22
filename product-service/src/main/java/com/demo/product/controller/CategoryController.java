package com.demo.product.controller;

import com.demo.product.service.CategoryService;
import com.demo.product.dto.CategoryDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> create(@Valid @RequestBody CategoryDTO in) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(in));
    }

    @GetMapping("/{id}")
    public CategoryDTO get(@PathVariable Long id) {
        return service.find(id);
    }

    @GetMapping
    public Page<CategoryDTO> list(Pageable p) {
        return service.list(p);
    }

    @PutMapping("/{id}")
    public CategoryDTO update(@PathVariable Long id, @Valid @RequestBody CategoryDTO in) {
        return service.update(id, in);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
