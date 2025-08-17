package com.demo.product.service;

import com.demo.product.model.Product;
import com.demo.product.repository.ProductRepository;
import com.demo.product.web.ProductDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository productRepository) {
        this.repository = productRepository;
    }

    public ProductDTO create(ProductDTO dto) {
        Product product = new Product(null, dto.name(), dto.price(), dto.inStock());
        return toDTO(repository.save(product));
    }

    public ProductDTO find(Long id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: %d".formatted(id)));
        return toDTO(product);
    }

    public ProductDTO update(Long id, ProductDTO dto) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: %d".formatted(id)));

        product.setName(dto.name());
        product.setPrice(dto.price());
        product.setInStock(dto.inStock());
        return toDTO(repository.save(product));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Product not found with id: %d".formatted(id));
        }
        repository.deleteById(id);
    }


    private ProductDTO toDTO(Product product) {
        return new ProductDTO(product.getId(), product.getName(), product.getPrice(), product.isInStock());
    }

    public Page<ProductDTO> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDTO);
    }
}
