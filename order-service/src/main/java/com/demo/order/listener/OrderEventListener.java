package com.demo.order.listener;

import com.demo.events.delivery.DeliveryTopics;
import com.demo.events.delivery.ShipmentCancelledEvent;
import com.demo.events.delivery.ShipmentCreatedEvent;
import com.demo.events.delivery.ShipmentDeliveredEvent;
import com.demo.events.delivery.ShipmentDispatchedEvent;
import com.demo.events.payment.PaymentCompletedEvent;
import com.demo.events.payment.PaymentFailedEvent;
import com.demo.events.payment.PaymentTopics;
import com.demo.events.payment.RefundCompletedEvent;
import com.demo.events.payment.RefundFailedEvent;
import com.demo.events.stock.StockReservationFailedEvent;
import com.demo.events.stock.StockTopics;
import com.demo.kafka.utils.entity.ProcessedEvent;
import com.demo.kafka.utils.repository.ProcessedEventRepository;
import com.demo.order.entity.OrderStatus;
import com.demo.order.repository.OrderRepository;
import com.demo.order.service.OrderService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Component
public class OrderEventListener {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final MeterRegistry meterRegistry;

    public OrderEventListener(OrderService orderService,
                               OrderRepository orderRepository,
                               ProcessedEventRepository processedEventRepository,
                               MeterRegistry meterRegistry) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.processedEventRepository = processedEventRepository;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(topics = StockTopics.STOCK_RESERVATION_FAILED, groupId = "order-service")
    @Transactional
    public void onStockReservationFailed(StockReservationFailedEvent event) {
        if (isDuplicate(event.eventId(), event.getClass().getSimpleName())) {
            return;
        }
        markProcessed(event.eventId(), event.getClass().getSimpleName());
        orderService.handleTransientFailure(event.orderId(), event.reason());
        count("stock-reservation-failed");
        log.info("Transient failure handled for order {} (stock reservation failed): {}", event.orderId(), event.reason());
    }

    @KafkaListener(topics = StockTopics.STOCK_RESERVATION_FAILED + ".dlq", groupId = "order-service-dlq")
    public void onStockReservationFailedDlq(StockReservationFailedEvent event) {
        log.error("DLQ: stock.reservation.failed.dlq event={}", event);
    }

    @KafkaListener(topics = PaymentTopics.PAYMENT_COMPLETED, groupId = "order-service")
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        if (isDuplicate(event.eventId(), event.getClass().getSimpleName())) {
            return;
        }
        markProcessed(event.eventId(), event.getClass().getSimpleName());
        orderService.confirm(event);
        count("payment-completed");
        log.info("Order {} confirmed after payment completed (ref={})", event.orderId(), event.paymentReference());
    }

    @KafkaListener(topics = PaymentTopics.PAYMENT_COMPLETED + ".dlq", groupId = "order-service-dlq")
    public void onPaymentCompletedDlq(PaymentCompletedEvent event) {
        log.error("DLQ: payment.completed.dlq event={}", event);
    }

    @KafkaListener(topics = PaymentTopics.PAYMENT_FAILED, groupId = "order-service")
    @Transactional
    public void onPaymentFailed(PaymentFailedEvent event) {
        if (isDuplicate(event.eventId(), event.getClass().getSimpleName())) {
            return;
        }
        markProcessed(event.eventId(), event.getClass().getSimpleName());
        orderService.cancel(event.orderId(), event.reason());
        count("payment-failed");
        log.info("Order {} cancelled due to payment failure: {}", event.orderId(), event.reason());
    }

    @KafkaListener(topics = PaymentTopics.PAYMENT_FAILED + ".dlq", groupId = "order-service-dlq")
    public void onPaymentFailedDlq(PaymentFailedEvent event) {
        log.error("DLQ: payment.failed.dlq event={}", event);
    }

    @KafkaListener(topics = DeliveryTopics.SHIPMENT_CREATED, groupId = "order-service")
    @Transactional
    public void onShipmentCreated(ShipmentCreatedEvent event) {
        if (isDuplicate(event.eventId(), event.getClass().getSimpleName())) {
            return;
        }
        markProcessed(event.eventId(), event.getClass().getSimpleName());
        orderRepository.findById(event.orderId()).ifPresentOrElse(order -> {
            if (order.getStatus() == OrderStatus.CONFIRMED) {
                order.setStatus(OrderStatus.SHIPPED);
                order.setShippedAt(event.occurredAt());
                orderRepository.save(order);
                count("shipment-created");
                log.info("Order {} shipped (tracking={}, carrier={})", event.orderId(), event.trackingNumber(), event.carrier());
            } else {
                log.warn("Unexpected status {} for shipment.created on order {}", order.getStatus(), event.orderId());
            }
        }, () -> log.warn("Order not found for shipment.created: orderId={}", event.orderId()));
    }

    @KafkaListener(topics = DeliveryTopics.SHIPMENT_CREATED + ".dlq", groupId = "order-service-dlq")
    public void onShipmentCreatedDlq(ShipmentCreatedEvent event) {
        log.error("DLQ: shipment.created.dlq event={}", event);
    }

    @KafkaListener(topics = DeliveryTopics.SHIPMENT_DISPATCHED, groupId = "order-service")
    @Transactional
    public void onShipmentDispatched(ShipmentDispatchedEvent event) {
        if (isDuplicate(event.eventId(), event.getClass().getSimpleName())) {
            return;
        }
        markProcessed(event.eventId(), event.getClass().getSimpleName());
        count("shipment-dispatched");
        log.info("Order {} shipment dispatched (tracking={})", event.orderId(), event.trackingNumber());
    }

    @KafkaListener(topics = DeliveryTopics.SHIPMENT_DISPATCHED + ".dlq", groupId = "order-service-dlq")
    public void onShipmentDispatchedDlq(ShipmentDispatchedEvent event) {
        log.error("DLQ: shipment.dispatched.dlq event={}", event);
    }

    @KafkaListener(topics = DeliveryTopics.SHIPMENT_DELIVERED, groupId = "order-service")
    @Transactional
    public void onShipmentDelivered(ShipmentDeliveredEvent event) {
        if (isDuplicate(event.eventId(), event.getClass().getSimpleName())) {
            return;
        }
        markProcessed(event.eventId(), event.getClass().getSimpleName());
        orderRepository.findById(event.orderId()).ifPresentOrElse(order -> {
            if (order.getStatus() == OrderStatus.SHIPPED) {
                order.setStatus(OrderStatus.DELIVERED);
                orderRepository.save(order);
                count("shipment-delivered");
                log.info("Order {} delivered", event.orderId());
            } else {
                log.warn("Unexpected status {} for shipment.delivered on order {}", order.getStatus(), event.orderId());
            }
        }, () -> log.warn("Order not found for shipment.delivered: orderId={}", event.orderId()));
    }

    @KafkaListener(topics = DeliveryTopics.SHIPMENT_DELIVERED + ".dlq", groupId = "order-service-dlq")
    public void onShipmentDeliveredDlq(ShipmentDeliveredEvent event) {
        log.error("DLQ: shipment.delivered.dlq event={}", event);
    }

    @KafkaListener(topics = DeliveryTopics.SHIPMENT_CANCELLED, groupId = "order-service")
    @Transactional
    public void onShipmentCancelled(ShipmentCancelledEvent event) {
        if (isDuplicate(event.eventId(), event.getClass().getSimpleName())) {
            return;
        }
        markProcessed(event.eventId(), event.getClass().getSimpleName());
        orderRepository.findById(event.orderId()).ifPresentOrElse(order -> {
            if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.SHIPPED) {
                order.setStatus(OrderStatus.REFUNDING);
                orderRepository.save(order);
                count("shipment-cancelled");
                log.info("Order {} moved to REFUNDING after shipment cancelled", event.orderId());
            } else {
                log.warn("Unexpected status {} for shipment.cancelled on order {}", order.getStatus(), event.orderId());
            }
        }, () -> log.warn("Order not found for shipment.cancelled: orderId={}", event.orderId()));
    }

    @KafkaListener(topics = DeliveryTopics.SHIPMENT_CANCELLED + ".dlq", groupId = "order-service-dlq")
    public void onShipmentCancelledDlq(ShipmentCancelledEvent event) {
        log.error("DLQ: shipment.cancelled.dlq event={}", event);
    }

    @KafkaListener(topics = PaymentTopics.REFUND_COMPLETED, groupId = "order-service")
    @Transactional
    public void onRefundCompleted(RefundCompletedEvent event) {
        if (isDuplicate(event.eventId(), event.getClass().getSimpleName())) {
            return;
        }
        markProcessed(event.eventId(), event.getClass().getSimpleName());
        orderRepository.findById(event.orderId()).ifPresentOrElse(order -> {
            if (order.getStatus() == OrderStatus.REFUNDING) {
                order.setStatus(OrderStatus.REFUNDED);
                orderRepository.save(order);
                count("refund-completed");
                log.info("Order {} refunded (ref={}, amount={})", event.orderId(), event.paymentReference(), event.amount());
            } else {
                log.warn("Unexpected status {} for refund.completed on order {}", order.getStatus(), event.orderId());
            }
        }, () -> log.warn("Order not found for refund.completed: orderId={}", event.orderId()));
    }

    @KafkaListener(topics = PaymentTopics.REFUND_COMPLETED + ".dlq", groupId = "order-service-dlq")
    public void onRefundCompletedDlq(RefundCompletedEvent event) {
        log.error("DLQ: refund.completed.dlq event={}", event);
    }

    @KafkaListener(topics = PaymentTopics.REFUND_FAILED, groupId = "order-service")
    @Transactional
    public void onRefundFailed(RefundFailedEvent event) {
        if (isDuplicate(event.eventId(), event.getClass().getSimpleName())) {
            return;
        }
        markProcessed(event.eventId(), event.getClass().getSimpleName());
        orderRepository.findById(event.orderId()).ifPresentOrElse(order -> {
            if (order.getStatus() == OrderStatus.REFUNDING) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancellationReason("Refund failed: " + event.reason());
                orderRepository.save(order);
                count("refund-failed");
                log.error("Refund failed for order {}, order marked CANCELLED: {}", event.orderId(), event.reason());
            } else {
                log.warn("Unexpected status {} for refund.failed on order {}", order.getStatus(), event.orderId());
            }
        }, () -> log.warn("Order not found for refund.failed: orderId={}", event.orderId()));
    }

    @KafkaListener(topics = PaymentTopics.REFUND_FAILED + ".dlq", groupId = "order-service-dlq")
    public void onRefundFailedDlq(RefundFailedEvent event) {
        log.error("DLQ: refund.failed.dlq event={}", event);
    }

    private boolean isDuplicate(UUID eventId, String eventType) {
        if (processedEventRepository.existsById(eventId)) {
            log.warn("Duplicate event skipped: eventId={}, type={}", eventId, eventType);
            meterRegistry.counter("order.event.duplicate", "service", "order-service", "event", eventType).increment();
            return true;
        }
        return false;
    }

    private void markProcessed(UUID eventId, String eventType) {
        processedEventRepository.save(new ProcessedEvent(eventId, eventType, OffsetDateTime.now()));
    }

    private void count(String eventName) {
        meterRegistry.counter("order.event.consumed", "service", "order-service", "event", eventName).increment();
    }
}
