package com.demo.order.service;

import com.demo.events.order.*;
import com.demo.events.order.OrderTopics;
import com.demo.events.payment.PaymentCompletedEvent;
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
    private static final List<OrderStatus> REQUEST_CANCELLATION_ELIGIBLE_STATUSES = List.of(
            OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.SHIPPED
    );
    private static final List<OrderStatus> CANCEL_ELIGIBLE_STATUSES = List.of(
            OrderStatus.PENDING, OrderStatus.AWAITING_PAYMENT
    );
    private static final int MAX_RETRY_COUNT = 3;

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
    public OrderOutDTO createOrder(OrderInDTO dto, String username) {
        log.info("Creating order for user {} with {} items", username, dto.orderItems().size());
        List<OrderItem> mergedOrderItems = dto.orderItems().stream().collect(Collectors.toMap(
                OrderItemInDTO::productId,
                Function.identity(),
                (a, b) -> new OrderItemInDTO(a.productId(), a.quantity() + b.quantity(), a.unitPrice()),
                LinkedHashMap::new)).values().stream()
                .map(orderMapper::toEntity)
                .toList();
        Order order = orderMapper.toEntity(dto);
        order.setOrderItems(mergedOrderItems);
        order.setUserId(username);
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
                saved.getUserId(),
                saved.getTotalAmount(),
                saved.getContactEmail(),
                saved.getPaymentMethodId(),
                saved.getCreatedAt(),
                saved.getOrderItems().stream()
                        .map(i -> new Item(Long.valueOf(i.getProductId()), i.getQuantity(), i.getUnitPrice()))
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

    public Page<OrderOutDTO> findAllByUser(String userId, Pageable pageable) {
        var from = OffsetDateTime.now().minusMonths(6);
        var to = OffsetDateTime.now();
        Specification<Order> spec = Specification
                .where(OrderSpecifications.userIdEquals(userId)                )
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
    public OrderOutDTO confirm(PaymentCompletedEvent event) {
        var id = event.orderId();
        log.info("Confirming order: id={}", id);
        var order = orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            log.error("Illegal status {} for the order with id: {}", order.getStatus(), id);
            throw new IllegalStateException("Only PENDING orders can be confirmed");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(event.occurredAt());
        order.setPaymentReference(event.paymentReference());
        var saved = orderRepository.save(order);
        log.info("Order confirmed: id={}", id);
        meterRegistry.counter("order.confirmed", "service", "order-service").increment();

        var evt = new OrderConfirmedEvent(
                UUID.randomUUID(),
                saved.getId(),
                event.paymentReference(),
                event.occurredAt());

        eventPublisher.publish(OrderTopics.ORDER_CONFIRMED, evt);
        log.debug("OrderConfirmedEvent published: orderId={}", saved.getId());

        return orderMapper.toOutDto(saved);
    }

    @Transactional
    public OrderOutDTO cancel(UUID id, String reason) {
        log.info("Cancelling order: id={}", id);
        var order = orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        var orderStatus = order.getStatus();
        if (!CANCEL_ELIGIBLE_STATUSES.contains(orderStatus)) {
            log.error("Cannot cancel {} order (not PENDING or AWAITING_PAYMENT): id={}", orderStatus, id);
            throw new IllegalStateException("Only PENDING or AWAITING_PAYMENT orders can be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        var saved = orderRepository.save(order);
        log.info("Order cancelled: id={}", id);
        meterRegistry.counter("order.cancelled", "service", "order-service").increment();

        var evt = new OrderCancelledEvent(
                UUID.randomUUID(),
                saved.getId(),
                reason,
                OffsetDateTime.now());

        eventPublisher.publish(OrderTopics.ORDER_CANCELLED, evt);
        log.debug("OrderCancelledEvent published: orderId={}", saved.getId());

        return orderMapper.toOutDto(saved);
    }

    @Transactional
    public void handleTransientFailure(UUID id, String reason) {
        log.info("Handling transient failure for order: id={}, reason={}", id, reason);
        var order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("handleTransientFailure called on non-PENDING order {}: status={}", id, order.getStatus());
            return;
        }
        order.setRetryCount(order.getRetryCount() + 1);
        boolean giveUp = order.getRetryCount() >= MAX_RETRY_COUNT
                || OffsetDateTime.now().isAfter(order.getExpiresAt());
        if (giveUp) {
            log.info("Order {} giving up after {} retries, cancelling (reason={})", id, order.getRetryCount(), reason);
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancellationReason(reason);
            orderRepository.save(order);
            meterRegistry.counter("order.cancelled", "service", "order-service").increment();
            var evt = new OrderCancelledEvent(UUID.randomUUID(), id, reason, OffsetDateTime.now());
            eventPublisher.publish(OrderTopics.ORDER_CANCELLED, evt);
            log.debug("OrderCancelledEvent published after retry exhaustion: orderId={}", id);
        } else {
            log.info("Order {} will be retried (retryCount={})", id, order.getRetryCount());
            orderRepository.save(order);
            meterRegistry.counter("order.retry", "service", "order-service").increment();
            var retryEvt = new OrderCreatedEvent(
                    UUID.randomUUID(),
                    order.getId(),
                    order.getUserId(),
                    order.getTotalAmount(),
                    order.getContactEmail(),
                    order.getPaymentMethodId(),
                    order.getCreatedAt(),
                    order.getOrderItems().stream()
                            .map(i -> new Item(Long.valueOf(i.getProductId()), i.getQuantity(), i.getUnitPrice()))
                            .toList());
            eventPublisher.publish(OrderTopics.ORDER_CREATED, retryEvt);
            log.debug("OrderCreatedEvent republished for retry: orderId={}, retryCount={}", id, order.getRetryCount());
        }
    }

    @Transactional
    public OrderOutDTO requestCancellation(UUID id, String reason) {
        log.info("Requesting cancellation for order: id={}", id);
        var order = orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        var orderStatus = order.getStatus();
        if (!REQUEST_CANCELLATION_ELIGIBLE_STATUSES.contains(orderStatus)) {
            log.error("Cannot request cancellation for {} order: id={}", orderStatus, id);
            throw new IllegalStateException("%s orders cannot request cancellation".formatted(orderStatus));
        }
        order.setStatus(OrderStatus.CANCELLATION_REQUESTED);
        order.setCancellationReason(reason);
        var saved = orderRepository.save(order);
        log.info("Cancellation requested for order: id={}", id);
        meterRegistry.counter("order.cancellation_requested", "service", "order-service").increment();

        var evt = new OrderCancellationRequestedEvent(
                UUID.randomUUID(),
                saved.getId(),
                reason,
                OffsetDateTime.now());

        eventPublisher.publish(OrderTopics.ORDER_CANCELLATION_REQUESTED, evt);
        log.debug("OrderCancellationRequestedEvent published: orderId={}", saved.getId());

        return orderMapper.toOutDto(saved);
    }

}
