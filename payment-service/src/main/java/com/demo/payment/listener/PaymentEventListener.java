package com.demo.payment.listener;

import com.demo.events.order.OrderCancellationRequestedEvent;
import com.demo.events.order.OrderTopics;
import com.demo.events.stock.StockReservedEvent;
import com.demo.events.stock.StockTopics;
import com.demo.kafka.utils.entity.ProcessedEvent;
import com.demo.kafka.utils.repository.ProcessedEventRepository;
import com.demo.payment.service.PaymentService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Component
public class PaymentEventListener {

    private final PaymentService paymentService;
    private final ProcessedEventRepository processedEventRepository;
    private final MeterRegistry meterRegistry;

    public PaymentEventListener(PaymentService paymentService,
                                ProcessedEventRepository processedEventRepository,
                                MeterRegistry meterRegistry) {
        this.paymentService = paymentService;
        this.processedEventRepository = processedEventRepository;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(topics = StockTopics.STOCK_RESERVED, groupId = "payment-service")
    @Transactional
    public void onStockReserved(StockReservedEvent event) {
        if (isDuplicate(event.eventId(), event.getClass().getSimpleName())) {
            return;
        }
        markProcessed(event.eventId(), event.getClass().getSimpleName());
        paymentService.processPayment(event);
        count("stock-reserved");
        log.info("Processed stock.reserved for orderId={}", event.orderId());
    }

    @KafkaListener(topics = StockTopics.STOCK_RESERVED + ".dlq", groupId = "payment-service-dlq")
    public void onStockReservedDlq(StockReservedEvent event) {
        log.error("DLQ: stock.reserved.dlq event={}", event);
    }

    @KafkaListener(topics = OrderTopics.ORDER_CANCELLATION_REQUESTED, groupId = "payment-service")
    @Transactional
    public void onOrderCancellationRequested(OrderCancellationRequestedEvent event) {
        if (isDuplicate(event.eventId(), event.getClass().getSimpleName())) return;
        markProcessed(event.eventId(), event.getClass().getSimpleName());
        paymentService.processRefund(event.orderId(), event.reason());
        count("order-cancellation-requested");
        log.info("Processed order.cancellation.requested for orderId={}", event.orderId());
    }

    @KafkaListener(topics = OrderTopics.ORDER_CANCELLATION_REQUESTED + ".dlq", groupId = "payment-service-dlq")
    public void onOrderCancellationRequestedDlq(OrderCancellationRequestedEvent event) {
        log.error("DLQ: order.cancellation.requested.dlq event={}", event);
    }

    private boolean isDuplicate(UUID eventId, String eventType) {
        if (processedEventRepository.existsById(eventId)) {
            log.warn("Duplicate event skipped: eventId={}, type={}", eventId, eventType);
            meterRegistry.counter("payment.event.duplicate",
                    "service", "payment-service", "type", eventType).increment();
            return true;
        }
        return false;
    }

    private void markProcessed(UUID eventId, String eventType) {
        processedEventRepository.save(new ProcessedEvent(eventId, eventType, OffsetDateTime.now()));
    }

    private void count(String eventName) {
        meterRegistry.counter("payment.event.processed",
                "service", "payment-service", "event", eventName).increment();
    }
}
