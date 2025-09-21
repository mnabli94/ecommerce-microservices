package com.demo.order.service;

import com.demo.order.client.ProductClient;
import com.demo.order.dto.in.OrderInDTO;
import com.demo.order.dto.out.OrderItemOutDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;
    private final MeterRegistry meterRegistry;

    public OrderService(OrderRepository orderRepository,
                        OrderMapper orderMapper,
                        ProductClient productClient, MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.productClient = productClient;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public OrderOutDTO createOrder(OrderInDTO dto) {
        Order order = orderMapper.toEntity(dto);
        order.setCreatedAt(OffsetDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        order.getOrderItems().forEach(item -> {
            setUpProduct(item);
            item.setOrder(order);
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                item.setQuantity(1);
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid unit price for product id: %s".formatted(item.getProductId()));
            }
        });
        order.calculateTotalAmount();
        Order saved = orderRepository.save(order);
        meterRegistry.counter("order.created", "service", "order-service").increment();
        return getOrderOutDTOWithProductDetails(saved);
    }

    public OrderOutDTO find(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: %s".formatted(id)));
        return getOrderOutDTOWithProductDetails(order);
    }

    public Page<OrderOutDTO> findAll(OrderStatus status, BigDecimal minAmount, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        Specification<Order> spec = Specification
                .where(OrderSpecifications.statusEquals(status))
                .and(OrderSpecifications.minAmount(minAmount))
                .and(OrderSpecifications.createdBetween(from, to));
        return orderRepository.findAll(spec, pageable)
                .map(this::getOrderOutDTOWithProductDetails);
    }

    private void setUpProduct(OrderItem item) {
        String productId = item.getProductId();
        ProductDTO product = productClient.getProduct(Long.parseLong(productId));
        validateProduct(productId, product);
        item.setUnitPrice(product.price());
    }

    private void validateProduct(String productId, ProductDTO product) {
        if (product == null || "Fallback Product".equals(product.name())) {
            meterRegistry.counter("order.validation_failed", "service", "order-service").increment();
            throw new EntityNotFoundException("Product not found or unavailable: " + productId);
        }

        if (!product.inStock()) {
            meterRegistry.counter("order.validation_failed", "service", "order-service").increment();
            throw new RuntimeException("Product not available: " + productId);
        }

        if (product.price() == null) {
            meterRegistry.counter("order.validation_failed", "service", "order-service").increment();
            throw new RuntimeException("Product price missing: " + productId);
        }
    }


    @Transactional
    public OrderOutDTO confirm(UUID id) {
        var order = orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be confirmed");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        var saved = orderRepository.save(order);
        meterRegistry.counter("order.confirmed", "service", "order-service").increment();
        return getOrderOutDTOWithProductDetails(saved);
    }

    @Transactional
    public OrderOutDTO cancel(UUID id) {
        var order = orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("CONFIRMED orders cannot be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        var saved = orderRepository.save(order);
        meterRegistry.counter("order.cancelled", "service", "order-service").increment();
        return getOrderOutDTOWithProductDetails(saved);
    }

    private OrderOutDTO getOrderOutDTOWithProductDetails(Order saved) {
        OrderOutDTO result = orderMapper.toOutDto(saved);
        Executor executor = Executors.newFixedThreadPool(16);
        List<CompletableFuture<ProductDTO>> futures = saved.getOrderItems().stream()
                .map(item -> CompletableFuture.supplyAsync(() ->
                        productClient.getProduct(Long.parseLong(item.getProductId())), executor))
                .toList();
        List<ProductDTO> products = futures.stream().map(CompletableFuture::join).toList();

        var finalOrderItems = saved.getOrderItems().stream().map(item -> {
            var pid = Long.valueOf(item.getProductId());
            var product = products.stream()
                    .filter(productDTO -> Objects.equals(productDTO.id(), pid))
                    .findFirst().orElseThrow(() -> new RuntimeException("product with id=%d is not more available"));
            var itemDto = orderMapper.toOutDto(item);
            return new OrderItemOutDTO(itemDto.id(), product, itemDto.quantity(), itemDto.unitPrice());
        }).toList();

        return new OrderOutDTO(result.id(), result.status(), result.shippingAddress(), finalOrderItems, result.totalAmount());
    }

}
