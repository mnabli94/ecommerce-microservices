package com.demo.order.listener;

import com.demo.events.delivery.DeliveryTopics;
import com.demo.events.delivery.ShipmentCancelledEvent;
import com.demo.events.delivery.ShipmentCreatedEvent;
import com.demo.events.delivery.ShipmentDeliveredEvent;
import com.demo.events.payment.PaymentCompletedEvent;
import com.demo.events.payment.PaymentFailedEvent;
import com.demo.events.payment.PaymentTopics;
import com.demo.events.payment.RefundCompletedEvent;
import com.demo.events.stock.StockReservationFailedEvent;
import com.demo.events.stock.StockTopics;
import com.demo.kafka.utils.producer.EventPublisher;
import com.demo.kafka.utils.repository.ProcessedEventRepository;
import com.demo.order.AbstractIntegrationTest;
import com.demo.order.entity.Order;
import com.demo.order.entity.OrderStatus;
import com.demo.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OrderEventListenerTest extends AbstractIntegrationTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void setUp() {
        processedEventRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void onStockReservationFailed_shouldCancelPendingOrder() {
        Order order = orderRepository.save(orderWithStatus(OrderStatus.PENDING));
        var event = new StockReservationFailedEvent(UUID.randomUUID(), order.getId(), "Out of stock", OffsetDateTime.now());

        eventPublisher.publish(StockTopics.STOCK_RESERVATION_FAILED, event);

        await().atMost(15, SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(updated.getCancellationReason()).isEqualTo("Out of stock");
        });
        assertThat(processedEventRepository.existsById(event.eventId())).isTrue();
    }

    @Test
    void onPaymentCompleted_shouldConfirmOrder() {
        Order order = orderRepository.save(orderWithStatus(OrderStatus.PENDING));
        var event = new PaymentCompletedEvent(UUID.randomUUID(), order.getId(), "PAY-REF-001", BigDecimal.valueOf(99.99), OffsetDateTime.now());

        eventPublisher.publish(PaymentTopics.PAYMENT_COMPLETED, event);

        await().atMost(15, SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(updated.getPaymentReference()).isEqualTo("PAY-REF-001");
        });
        assertThat(processedEventRepository.existsById(event.eventId())).isTrue();
    }

    @Test
    void onPaymentFailed_shouldCancelPendingOrder() {
        Order order = orderRepository.save(orderWithStatus(OrderStatus.PENDING));
        var event = new PaymentFailedEvent(UUID.randomUUID(), order.getId(), "Insufficient funds", OffsetDateTime.now());

        eventPublisher.publish(PaymentTopics.PAYMENT_FAILED, event);

        await().atMost(15, SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(updated.getCancellationReason()).isEqualTo("Insufficient funds");
        });
        assertThat(processedEventRepository.existsById(event.eventId())).isTrue();
    }

    @Test
    void onShipmentCreated_shouldShipConfirmedOrder() {
        Order order = orderRepository.save(orderWithStatus(OrderStatus.CONFIRMED));
        var event = new ShipmentCreatedEvent(UUID.randomUUID(), order.getId(), "TRK-001", "DHL",
                LocalDate.now().plusDays(3), OffsetDateTime.now());

        eventPublisher.publish(DeliveryTopics.SHIPMENT_CREATED, event);

        await().atMost(15, SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.SHIPPED);
            assertThat(updated.getShippedAt()).isNotNull();
        });
        assertThat(processedEventRepository.existsById(event.eventId())).isTrue();
    }

    @Test
    void onShipmentDelivered_shouldDeliverShippedOrder() {
        Order order = orderRepository.save(orderWithStatus(OrderStatus.SHIPPED));
        var event = new ShipmentDeliveredEvent(UUID.randomUUID(), order.getId(), OffsetDateTime.now());

        eventPublisher.publish(DeliveryTopics.SHIPMENT_DELIVERED, event);

        await().atMost(15, SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        });
        assertThat(processedEventRepository.existsById(event.eventId())).isTrue();
    }

    @Test
    void onShipmentCancelled_shouldSetRefunding() {
        Order order = orderRepository.save(orderWithStatus(OrderStatus.SHIPPED));
        var event = new ShipmentCancelledEvent(UUID.randomUUID(), order.getId(), OffsetDateTime.now());

        eventPublisher.publish(DeliveryTopics.SHIPMENT_CANCELLED, event);

        await().atMost(15, SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.REFUNDING);
        });
        assertThat(processedEventRepository.existsById(event.eventId())).isTrue();
    }

    @Test
    void onRefundCompleted_shouldSetRefunded() {
        Order order = orderRepository.save(orderWithStatus(OrderStatus.REFUNDING));
        var event = new RefundCompletedEvent(UUID.randomUUID(), order.getId(), "REF-001",
                BigDecimal.valueOf(99.99), OffsetDateTime.now());

        eventPublisher.publish(PaymentTopics.REFUND_COMPLETED, event);

        await().atMost(15, SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        });
        assertThat(processedEventRepository.existsById(event.eventId())).isTrue();
    }

    @Test
    void duplicateEvent_shouldBeSkipped() {
        Order order = orderRepository.save(orderWithStatus(OrderStatus.PENDING));
        UUID eventId = UUID.randomUUID();
        var event = new StockReservationFailedEvent(eventId, order.getId(), "Out of stock", OffsetDateTime.now());

        // First publish — order should be cancelled
        eventPublisher.publish(StockTopics.STOCK_RESERVATION_FAILED, event);
        await().atMost(15, SECONDS).untilAsserted(() ->
                assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                        .isEqualTo(OrderStatus.CANCELLED)
        );

        // Reset order to PENDING to verify the second event is skipped
        Order reset = orderRepository.findById(order.getId()).orElseThrow();
        reset.setStatus(OrderStatus.PENDING);
        orderRepository.save(reset);

        // Second publish with same eventId — duplicate, should be skipped
        eventPublisher.publish(StockTopics.STOCK_RESERVATION_FAILED, event);

        await().during(3, SECONDS).atMost(5, SECONDS).untilAsserted(() ->
                assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                        .isEqualTo(OrderStatus.PENDING)
        );
    }

    private Order orderWithStatus(OrderStatus status) {
        Order order = new Order();
        order.setStatus(status);
        order.setTotalAmount(BigDecimal.valueOf(99.99));
        return order;
    }
}
