package com.demo.order.scheduler;

import com.demo.order.entity.OrderStatus;
import com.demo.order.repository.OrderRepository;
import com.demo.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderExpirationScheduler {

    private static final List<OrderStatus> EXPIRABLE_STATUSES = List.of(
            OrderStatus.PENDING, OrderStatus.AWAITING_PAYMENT
    );

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public OrderExpirationScheduler(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Scheduled(fixedRateString = "${order.scheduler.expiration-check-ms:60000}")
    @Transactional
    public void expireStaleOrders() {
        var expired = orderRepository.findByStatusInAndExpiresAtBefore(EXPIRABLE_STATUSES, OffsetDateTime.now());
        if (expired.isEmpty()) {
            return;
        }
        log.info("Expiring {} stale order(s)", expired.size());
        expired.forEach(order -> {
            log.info("Expiring order id={}, status={}, expiresAt={}", order.getId(), order.getStatus(), order.getExpiresAt());
            orderService.cancel(order.getId(), "Order expired: maximum wait time exceeded");
        });
    }
}
