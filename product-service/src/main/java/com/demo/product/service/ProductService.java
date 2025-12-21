package com.demo.product.service;

import com.demo.events.order.OrderConfirmedEvent;
import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.order.OrderTopics;
import com.demo.kafka.utils.EventConsumer;
import com.demo.product.mapper.ProductMapper;
import com.demo.product.entity.Category;
import com.demo.product.entity.Product;
import com.demo.product.repository.CategoryRepository;
import com.demo.product.repository.ProductRepository;
import com.demo.product.dto.ProductDTO;
import com.demo.product.repository.ProductSpecifications;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final EventConsumer<OrderConfirmedEvent> orderConfirmedEventConsumer;
    private final EventConsumer<OrderCreatedEvent> orderCreatedEventConsumer;
    private final MeterRegistry meterRegistry;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository, EventConsumer<OrderConfirmedEvent> orderConfirmedEventConsumer, EventConsumer<OrderCreatedEvent> orderCreatedEventConsumer, MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.orderConfirmedEventConsumer = orderConfirmedEventConsumer;
        this.orderCreatedEventConsumer = orderCreatedEventConsumer;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void init() {
        orderCreatedEventConsumer.registerWithDlqRecord(
                OrderTopics.ORDER_CREATED,
                "product-service",
                OrderTopics.ORDER_CREATED + ".dlq",
                OrderCreatedEvent.class,
                this::onOrderCreated
        );

        orderConfirmedEventConsumer.registerWithDlqRecord(
                OrderTopics.ORDER_CONFIRMED,
                "product-service",
                OrderTopics.ORDER_CONFIRMED + ".dlq",
                OrderConfirmedEvent.class,
                this::onOrderConfirmed
        );


//        orderCreatedEventConsumer.registerWithDlq(OrderTopics.ORDER_CREATED, "product-service", OrderCreatedEvent.class, this::onOrderCreated);
//        orderConfirmedEventConsumer.registerWithDlq(OrderTopics.ORDER_CONFIRMED, "product-service", OrderConfirmedEvent.class, this::onOrderConfirmed);
//        orderCreatedEventConsumer.registerDlqConsumer("order.created.dlq", "product-service-dlq", OrderCreatedEvent.class,
//                event -> logger.error("DLQ event received: {}", event));
//        orderConfirmedEventConsumer.registerDlqConsumer("order.confirmed.dlq", "product-service-dlq", OrderConfirmedEvent.class,
//                event -> logger.error("DLQ event received: {}", event));
    }

    private void onOrderConfirmed(ConsumerRecord<String, OrderConfirmedEvent> record) {
        var event = record.value();

        logger.info("Received OrderCreated: kafkaKey={}, eventId={}, paymentReference={}, occurredAt={}",
                record.key(), event.key(), event.paymentReference(), event.occurredAt());
    }

    private void onOrderCreated(ConsumerRecord<String, OrderCreatedEvent> record) {
        var event = record.value();
        if (event.items().stream().mapToInt(OrderCreatedEvent.Item::quantity).sum() > 10) {
            throw new RuntimeException("Too much quantity");
        }
        if (event.totalAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
            throw new RuntimeException("Too much total amunt");
        }
        logger.info("Received OrderCreated: kafkaKey={}, eventId={}, total={}, occurredAt={}",
                record.key(), event.key(), event.totalAmount(), event.occurredAt());
    }


//    private void onOrderCreated(OrderCreatedEvent event) {
////        if (event.items().stream().mapToInt(OrderCreatedEvent.Item::quantity).sum() > 10) {
////            throw new RuntimeException("Too much quantity");
////        }
//        logger.info("OrderCreated -  event = {}", event);
//        meterRegistry.counter("order.event.consumed", "service", "order-service", "event", "order-created").increment();
//        logger.info("Received OrderCreated: key={}, total={}, .occurredAt={}", event.key(), event.totalAmount(), event.occurredAt());
//    }
//
//    private void onOrderConfirmed(OrderConfirmedEvent event) {

    /// /        if (event.totalAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
    /// /            throw new RuntimeException("Too much total amunt");
    /// /        }
//        logger.info("onOrderCreated event = payload ={}",event);
//        meterRegistry.counter("order.event.consumed", "service", "order-service", "event", "order-confirmed").increment();
//        logger.info("Received OrderConfirmed: key={}, createdAt={}", event.key(), event.occurredAt());
//    }
    public ProductDTO create(ProductDTO dto) {
        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: %d".formatted(dto.categoryId())));
        Product product = new Product(null, dto.name(), dto.price(), dto.inStock(), category);
        return ProductMapper.toDto(productRepository.save(product));
    }

    public ProductDTO find(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: %d".formatted(id)));
        return ProductMapper.toDto(product);
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
        return ProductMapper.toDto(productRepository.save(product));
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new EntityNotFoundException("Product not found with id: %d".formatted(id));
        }
        productRepository.deleteById(id);
    }

    public Page<ProductDTO> findAll(String name,
                                    Long categoryId,
                                    BigDecimal minPrice,
                                    BigDecimal maxPrice,
                                    Pageable pageable) {
        Specification<Product> spec = Specification
                .where(ProductSpecifications.categoryIdEquals(categoryId))
                .and(ProductSpecifications.nameContains(name))
                .and(ProductSpecifications.minPrice(minPrice))
                .and(ProductSpecifications.maxPrice(maxPrice));
        return productRepository.findAll(spec, pageable).map(ProductMapper::toDto);
    }
}
