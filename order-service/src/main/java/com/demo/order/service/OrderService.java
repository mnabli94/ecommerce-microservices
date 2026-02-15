package com.demo.order.service;

import com.demo.events.order.*;
import com.demo.events.order.OrderTopics;
import com.demo.kafka.utils.producer.EventPublisher;
import com.demo.order.dto.in.OrderInDTO;
import com.demo.order.dto.in.OrderItemInDTO;
import com.demo.order.dto.out.OrderOutDTO;
import com.demo.order.dto.out.ProductDTO;
import com.demo.order.entity.Order;
import com.demo.order.entity.OrderItem;
import com.demo.order.entity.OrderStatus;
import com.demo.order.mapper.OrderMapper;
import com.demo.order.repository.OrderRepository;
import com.demo.order.repository.OrderSpecifications;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductCaller productCaller;
    private final MeterRegistry meterRegistry;
    private final EventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository,
            OrderMapper orderMapper,
            ProductCaller productCaller,
            MeterRegistry meterRegistry,
            EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.productCaller = productCaller;
        this.meterRegistry = meterRegistry;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderOutDTO createOrder(OrderInDTO dto) {
        log.info("Creating order with {} items", dto.orderItems().size());
        List<OrderItem> mergedOrderItems = dto.orderItems().stream().collect(Collectors.toMap(
                OrderItemInDTO::productId,
                Function.identity(),
                (a, b) -> new OrderItemInDTO(a.productId(), a.quantity() + b.quantity(), a.unitPrice()),
                LinkedHashMap::new)).values().stream()
                .map(orderMapper::toEntity)
                .toList();
        Order order = orderMapper.toEntity(dto);
        order.setOrderItems(mergedOrderItems);
        order.setUserId(currentUsername());
        order.setCreatedAt(OffsetDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        order.getOrderItems().forEach(item -> {
            setUpProduct(item);
            item.setOrder(order);
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                item.setQuantity(1);
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Invalid unit price for product id: %s".formatted(item.getProductId()));
            }
        });
        order.calculateTotalAmount();
        Order saved = orderRepository.save(order);
        log.info("Order created: id={}, totalAmount={}", saved.getId(), saved.getTotalAmount());
        meterRegistry.counter("order.created", "service", "order-service").increment();

        var evt = new OrderCreatedEvent(
                UUID.randomUUID(),
                saved.getId(),
                UUID.randomUUID(), // placeholder — event contract requires UUID, JWT only has username
                saved.getTotalAmount(),
                saved.getCreatedAt(),
                saved.getOrderItems().stream()
                        .map(i -> new OrderCreatedEvent.Item(Long.valueOf(i.getProductId()), i.getQuantity(),
                                i.getUnitPrice()))
                        .toList());

        eventPublisher.publish(OrderTopics.ORDER_CREATED, evt);
        log.debug("OrderCreatedEvent published: orderId={}", saved.getId());
        return orderMapper.toOutDto(saved);
    }

    public OrderOutDTO find(UUID id) {
        log.debug("Finding order by id: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: %s".formatted(id)));
        return orderMapper.toOutDto(order);
    }

    public Page<OrderOutDTO> findAll(OrderStatus status, BigDecimal minAmount, OffsetDateTime from, OffsetDateTime to,
            Pageable pageable) {
        Specification<Order> spec = Specification
                .where(OrderSpecifications.statusEquals(status))
                .and(OrderSpecifications.minAmount(minAmount))
                .and(OrderSpecifications.createdBetween(from, to));
        return orderRepository.findAll(spec, pageable)
                .map(orderMapper::toOutDto);
    }

    private void setUpProduct(OrderItem item) {
        String productId = item.getProductId();
        ProductDTO product = productCaller.getProduct(Long.parseLong(productId));
        validateProduct(productId, product);
        item.setUnitPrice(product.price());
        item.setProductName(product.name());
    }

    private void validateProduct(String productId, ProductDTO product) {
        if (product == null) {
            meterRegistry.counter("order.validation_failed", "service", "order-service").increment();
            log.error("Product not found: {}", productId);
            throw new EntityNotFoundException("Product not found: " + productId);
        }

        if (!product.inStock()) {
            meterRegistry.counter("order.validation_failed", "service", "order-service").increment();
            log.error("Product not available: {}", productId);
            throw new IllegalStateException("Product not available: " + productId);
        }

        if (product.price() == null) {
            meterRegistry.counter("order.validation_failed", "service", "order-service").increment();
            log.error("Product price missing: {}", product);
            throw new IllegalStateException("Product price missing: " + productId);
        }
    }

    @Transactional
    public OrderOutDTO confirm(UUID id) {
        log.info("Confirming order: id={}", id);
        var order = orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            log.error("Illegal status {} for the order with id: {}", order.getStatus(), id);
            throw new IllegalStateException("Only PENDING orders can be confirmed");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        var saved = orderRepository.save(order);
        log.info("Order confirmed: id={}", id);
        meterRegistry.counter("order.confirmed", "service", "order-service").increment();

        var evt = new OrderConfirmedEvent(
                UUID.randomUUID(),
                saved.getId(),
                UUID.randomUUID().toString(), // placeholder — payment integration pending
                saved.getCreatedAt());

        eventPublisher.publish(OrderTopics.ORDER_CONFIRMED, evt);
        log.debug("OrderConfirmedEvent published: orderId={}", saved.getId());

        return orderMapper.toOutDto(saved);
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jat) {
            return jat.getToken().getSubject();
        }
        throw new IllegalStateException("No authenticated user in SecurityContext");
    }

    @Transactional
    public OrderOutDTO cancel(UUID id) {
        log.info("Cancelling order: id={}", id);
        var order = orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            log.error("Cannot cancel CONFIRMED order: id={}", id);
            throw new IllegalStateException("CONFIRMED orders cannot be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        var saved = orderRepository.save(order);
        log.info("Order cancelled: id={}", id);
        meterRegistry.counter("order.cancelled", "service", "order-service").increment();
        return orderMapper.toOutDto(saved);
    }


}
