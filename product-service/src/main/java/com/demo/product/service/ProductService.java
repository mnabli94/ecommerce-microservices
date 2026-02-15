package com.demo.product.service;

import com.demo.events.order.OrderConfirmedEvent;
import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.order.OrderTopics;
import com.demo.kafka.utils.EventConsumer;
import com.demo.product.mapper.ProductMapper;
import com.demo.product.entity.Category;
import com.demo.product.entity.ProcessedEvent;
import com.demo.product.entity.Product;
import com.demo.product.repository.CategoryRepository;
import com.demo.product.repository.ProcessedEventRepository;
import com.demo.product.repository.ProductRepository;
import com.demo.product.dto.ProductDTO;
import com.demo.product.repository.ProductSpecifications;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ProductService {

        private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
        private final ProductRepository productRepository;
        private final CategoryRepository categoryRepository;
        private final ProcessedEventRepository processedEventRepository;
        private final EventConsumer<OrderConfirmedEvent> orderConfirmedEventConsumer;
        private final EventConsumer<OrderCreatedEvent> orderCreatedEventConsumer;
        private final MeterRegistry meterRegistry;

        public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                        ProcessedEventRepository processedEventRepository,
                        EventConsumer<OrderConfirmedEvent> orderConfirmedEventConsumer,
                        EventConsumer<OrderCreatedEvent> orderCreatedEventConsumer, MeterRegistry meterRegistry) {
                this.productRepository = productRepository;
                this.categoryRepository = categoryRepository;
                this.processedEventRepository = processedEventRepository;
                this.orderConfirmedEventConsumer = orderConfirmedEventConsumer;
                this.orderCreatedEventConsumer = orderCreatedEventConsumer;
                this.meterRegistry = meterRegistry;
        }

        @PostConstruct
        void init() {
                orderCreatedEventConsumer.registerWithDlq(OrderTopics.ORDER_CREATED, "product-service",
                                OrderCreatedEvent.class,
                                this::onOrderCreated);
                orderConfirmedEventConsumer.registerWithDlq(OrderTopics.ORDER_CONFIRMED, "product-service",
                                OrderConfirmedEvent.class, this::onOrderConfirmed);
                orderCreatedEventConsumer.registerDlqConsumer("order.created.dlq", "product-service-dlq",
                                OrderCreatedEvent.class,
                                event -> logger.error("DLQ event received: {}", event));
                orderConfirmedEventConsumer.registerDlqConsumer("order.confirmed.dlq", "product-service-dlq",
                                OrderConfirmedEvent.class,
                                event -> logger.error("DLQ event received: {}", event));
        }

        private void onOrderCreated(OrderCreatedEvent event) {
                if (isDuplicate(event.eventId(), "OrderCreatedEvent")) {
                        return;
                }
                logger.info("Received OrderCreated: key={}, total={}, occurredAt={}", event.key(), event.totalAmount(),
                                event.occurredAt());
                meterRegistry.counter("order.event.consumed", "service", "product-service", "event", "order-created")
                                .increment();
                markProcessed(event.eventId(), "OrderCreatedEvent");
        }

        private void onOrderConfirmed(OrderConfirmedEvent event) {
                if (isDuplicate(event.eventId(), "OrderConfirmedEvent")) {
                        return;
                }
                logger.info("Received OrderConfirmed: key={}, createdAt={}", event.key(), event.occurredAt());
                meterRegistry.counter("order.event.consumed", "service", "product-service", "event", "order-confirmed")
                                .increment();
                markProcessed(event.eventId(), "OrderConfirmedEvent");
        }

        private boolean isDuplicate(UUID eventId, String eventType) {
                if (processedEventRepository.existsById(eventId)) {
                        logger.warn("Duplicate event skipped: eventId={}, type={}", eventId, eventType);
                        meterRegistry.counter("order.event.duplicate", "service", "product-service", "event", eventType)
                                        .increment();
                        return true;
                }
                return false;
        }

        private void markProcessed(UUID eventId, String eventType) {
                processedEventRepository.save(new ProcessedEvent(eventId, eventType, OffsetDateTime.now()));
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
