package com.demo.product.service;

import com.demo.product.mapper.ProductMapper;
import com.demo.product.entity.Category;
import com.demo.product.entity.Product;
import com.demo.product.repository.CategoryRepository;
import com.demo.product.repository.ProductRepository;
import com.demo.product.dto.ProductDTO;
import com.demo.product.repository.ProductSpecifications;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProductService {

        private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
        private final ProductRepository productRepository;
        private final CategoryRepository categoryRepository;

        public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
                this.productRepository = productRepository;
                this.categoryRepository = categoryRepository;
        }

        @Transactional
        public ProductDTO create(ProductDTO dto) {
                logger.info("Creating product: name={}, categoryId={}", dto.name(), dto.categoryId());
                Category category = categoryRepository.findById(dto.categoryId())
                                .orElseThrow(() -> {
                                        logger.error("Category not found: id={}", dto.categoryId());
                                        return new EntityNotFoundException(
                                                        "Category not found with id: %d".formatted(dto.categoryId()));
                                });
                Product product = new Product(null, dto.name(), dto.price(), dto.inStock(), category);
                ProductDTO saved = ProductMapper.toDto(productRepository.save(product));
                logger.info("Product created successfully: id={}", saved.id());
                return saved;
        }

        public ProductDTO find(Long id) {
                logger.debug("Finding product by id: {}", id);
                Product product = productRepository.findById(id)
                                .orElseThrow(() -> {
                                        logger.error("Product not found: id={}", id);
                                        return new EntityNotFoundException(
                                                        "Product not found with id: %d".formatted(id));
                                });
                return ProductMapper.toDto(product);
        }

        @Transactional
        public ProductDTO update(Long id, ProductDTO dto) {
                logger.info("Updating product: id={}", id);
                Product product = productRepository.findById(id)
                                .orElseThrow(() -> {
                                        logger.error("Product not found for update: id={}", id);
                                        return new EntityNotFoundException(
                                                        "Product not found with id: %d".formatted(id));
                                });

                Category category = categoryRepository.findById(dto.categoryId())
                                .orElseThrow(() -> {
                                        logger.error("Category not found for product update: categoryId={}",
                                                        dto.categoryId());
                                        return new EntityNotFoundException(
                                                        "Category not found with id: %d".formatted(dto.categoryId()));
                                });

                product.setName(dto.name());
                product.setPrice(dto.price());
                product.setInStock(dto.inStock());
                product.setCategory(category);
                logger.info("Product updated successfully: id={}", id);
                return ProductMapper.toDto(productRepository.save(product));
        }

        @Transactional
        public void delete(Long id) {
                logger.info("Deleting product: id={}", id);
                if (!productRepository.existsById(id)) {
                        logger.error("Product not found for deletion: id={}", id);
                        throw new EntityNotFoundException("Product not found with id: %d".formatted(id));
                }
                productRepository.deleteById(id);
                logger.info("Product deleted successfully: id={}", id);
        }

        public Page<ProductDTO> findAll(String name,
                        Long categoryId,
                        BigDecimal minPrice,
                        BigDecimal maxPrice,
                        Pageable pageable) {
                logger.debug("Finding products: name={}, categoryId={}, minPrice={}, maxPrice={}", name, categoryId,
                                minPrice,
                                maxPrice);
                Specification<Product> spec = Specification
                                .where(ProductSpecifications.categoryIdEquals(categoryId))
                                .and(ProductSpecifications.nameContains(name))
                                .and(ProductSpecifications.minPrice(minPrice))
                                .and(ProductSpecifications.maxPrice(maxPrice));
                return productRepository.findAll(spec, pageable).map(ProductMapper::toDto);
        }
}
