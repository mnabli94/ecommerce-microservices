package com.demo.product.service;

import com.demo.product.model.Category;
import com.demo.product.model.Product;
import com.demo.product.repository.CategoryRepository;
import com.demo.product.repository.ProductRepository;
import com.demo.product.dto.ProductDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductDTO create(ProductDTO dto) {
        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: %d".formatted(dto.categoryId())));
        Product product = new Product(null, dto.name(), dto.price(), dto.inStock(), category);
        return toDTO(productRepository.save(product));
    }

    public ProductDTO find(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: %d".formatted(id)));
        return toDTO(product);
    }

    public ProductDTO update(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: %d".formatted(id)));

        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: %d".formatted(dto.categoryId())));

        product.setName(dto.name());
        product.setPrice(dto.price());
        product.setInStock(dto.inStock());
        product.setCategory(category);
        return toDTO(productRepository.save(product));
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new EntityNotFoundException("Product not found with id: %d".formatted(id));
        }
        productRepository.deleteById(id);
    }


    private ProductDTO toDTO(Product product) {
        Long categoryId = (product.getCategory() != null ? product.getCategory().getId() : null);
        return new ProductDTO(product.getId(), product.getName(), product.getPrice(), product.isInStock(), categoryId);
    }

    public Page<ProductDTO> findAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toDTO);
    }
}
