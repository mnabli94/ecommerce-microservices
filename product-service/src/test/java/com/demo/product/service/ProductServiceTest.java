package com.demo.product.service;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.product.dto.ProductDTO;
import com.demo.product.mapper.ProductMapper;
import com.demo.product.entity.Category;
import com.demo.product.entity.Product;
import com.demo.product.repository.CategoryRepository;
import com.demo.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @InjectMocks
    private ProductService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    private ProductDTO dto;
    private Category category;


    @BeforeEach
    void setUp() {
        category = new Category(1L, "Electronics");
        dto = new ProductDTO(null, "Laptop", new BigDecimal("19.90"), true, 1L);
    }

    @Test
    void create_shouldReturnSuccess() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocationOnMock -> {
            Product in = invocationOnMock.getArgument(0);
            return new Product(1L, in.getName(), in.getPrice(), in.isInStock(), in.getCategory());
        });

        ProductDTO created = productService.create(dto);

        verify(categoryRepository).findById(1L);
        verify(productRepository).save(productCaptor.capture());

        Product saved = productCaptor.getValue();

        assertAll(
                () -> assertEquals("Laptop", saved.getName()),
                () -> assertEquals(0, saved.getPrice().compareTo(new BigDecimal("19.90"))),
                () -> assertTrue(saved.isInStock()),
                () -> assertEquals(1L, saved.getCategory().getId()),
                () -> assertNotNull(created.id()),
                () -> assertEquals(1L, created.categoryId())
        );

        verifyNoMoreInteractions(categoryRepository, productRepository);
    }

    @Test
    void create_shouldThrow_whenCategoryIsNull() {
        dto = new ProductDTO(null, "Laptop", new BigDecimal("19.90"), true, null);
        assertThrows(EntityNotFoundException.class, () -> productService.create(dto));
    }

    @Test
    void createProduct_shouldThrow_whenCategoryNotFound() {
        ProductDTO input = new ProductDTO(null, "Laptop", new BigDecimal("19.90"), true, 99L);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> productService.create(input));

        verify(categoryRepository).findById(99L);
        verifyNoMoreInteractions(categoryRepository);
        verifyNoInteractions(productRepository);
    }

    @Test
    void find_shouldReturnDTO() {
        Product product = new Product(1L, "Laptop", new BigDecimal("19.90"), true, category);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDTO returnedDTO = productService.find(1L);

        assertEquals(1L, returnedDTO.id());
        assertEquals("Laptop", returnedDTO.name());
        assertEquals(1L, returnedDTO.categoryId());
        assertEquals(0, returnedDTO.price().compareTo(new BigDecimal("19.90")));

        verify(productRepository, times(1)).findById(1L);
        verifyNoInteractions(categoryRepository);

    }

    @Test
    void update_shouldReturnDTO() {

        Category initialCategory = new Category(1L, "Cat2");
        Product initialProduct = new Product(1L, "HP", new BigDecimal("19.90"), true, initialCategory);

        when(productRepository.findById(1L)).thenReturn(Optional.of(initialProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(initialCategory));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProductDTO returnedDTO = productService.update(1L, dto);

        assertEquals(1L, returnedDTO.id());
        assertEquals(1L, returnedDTO.categoryId());
        assertEquals("Laptop", returnedDTO.name());

        verify(productRepository).save(initialProduct);
    }


    @Test
    void deleteProduct_shouldReturnSuccess() {
        when(productRepository.existsById(1L)).thenReturn(true);
        productService.delete(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProduct_whenNotExists_shouldThrow() {
        when(productRepository.existsById(1L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> productService.delete(1L));

        verify(productRepository).existsById(1L);
        verifyNoMoreInteractions(productRepository);
    }


    @Test
    void findAll_shouldReturnPageOfProducts() {
        Category category = new Category(1L, "Cat2");
        var products = List.of(
                new Product(1L, "Phone", new BigDecimal("299.99"), true, category)
        );
        Pageable pageable = PageRequest.of(0,5);
        var page = new PageImpl<>(products, pageable, products.size());

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<ProductDTO> output = productService.findAll("Phone", null, null, null, pageable);
        assertEquals( 1, output.getTotalElements());
        assertEquals("Phone", output.getContent().get(0).name());

    }

    @Test
    void shouldSerializeAndDeserializeOrderCreatedEvent() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("12.50"),
                OffsetDateTime.now(),
                List.of(new OrderCreatedEvent.Item(1L, 2, new BigDecimal("6.25")))
        );

        String json = mapper.writeValueAsString(event);
        OrderCreatedEvent back = mapper.readValue(json, OrderCreatedEvent.class);

        assertNotNull(back.key());
        assertNotNull(back.orderId());
        assertNotNull(back.occurredAt());
        assertEquals(event.totalAmount(), back.totalAmount());
        assertEquals(1, back.items().size());
    }
}