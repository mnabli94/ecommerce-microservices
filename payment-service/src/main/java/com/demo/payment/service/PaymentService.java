package com.demo.payment.service;

import com.demo.events.payment.PaymentCompletedEvent;
import com.demo.events.payment.PaymentFailedEvent;
import com.demo.events.payment.PaymentTopics;
import com.demo.events.payment.RefundCompletedEvent;
import com.demo.events.payment.RefundFailedEvent;
import com.demo.events.stock.StockReservedEvent;
import com.demo.kafka.utils.producer.EventPublisher;
import com.demo.payment.dto.PaymentDTO;
import com.demo.payment.entity.Payment;
import com.demo.payment.entity.PaymentStatus;
import com.demo.payment.mapper.PaymentMapper;
import com.demo.payment.repository.PaymentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    public PaymentService(PaymentRepository paymentRepository,
                          EventPublisher eventPublisher,
                          MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void processPayment(StockReservedEvent event) {
        log.info("Processing payment for orderId={}, amount={}", event.orderId(), event.totalAmount());

        Optional<Payment> existing = paymentRepository.findByOrderId(event.orderId());
        if (existing.isPresent()) {
            log.warn("Payment already exists for orderId={}, skipping", event.orderId());
            return;
        }

        Payment payment = new Payment();
        payment.setOrderId(event.orderId());
        payment.setUserId(event.userId());
        payment.setAmount(event.totalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void processRefund(UUID orderId, String reason) {
        log.info("Processing refund for orderId={}, reason={}", orderId, reason);

        Optional<Payment> optPayment = paymentRepository.findByOrderIdForUpdate(orderId);
        if (optPayment.isEmpty()) {
            log.warn("No payment found for orderId={}, publishing refund.failed", orderId);
            publishRefundFailed(orderId, "No payment found for order");
            return;
        }

        Payment payment = optPayment.get();
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            log.warn("Payment for orderId={} is not COMPLETED (status={}), publishing refund.failed",
                    orderId, payment.getStatus());
            publishRefundFailed(orderId, "Payment not in COMPLETED state: " + payment.getStatus());
            return;
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        var evt = new RefundCompletedEvent(
                UUID.randomUUID(), orderId, payment.getPaymentReference(),
                payment.getAmount(), OffsetDateTime.now());
        eventPublisher.publish(PaymentTopics.REFUND_COMPLETED, evt);
        meterRegistry.counter("payment.refund.completed", "service", "payment-service").increment();
        log.info("Refund completed for orderId={}, ref={}", orderId, payment.getPaymentReference());
    }

    @Transactional
    public void completeFromWebhook(UUID orderId) {
        log.info("Webhook: completing payment for orderId={}", orderId);

        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found for orderId=" + orderId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot complete payment: current status is " + payment.getStatus());
        }

        completePayment(payment, payment.getAmount());
    }

    @Transactional
    public void failFromWebhook(UUID orderId, String reason) {
        log.info("Webhook: failing payment for orderId={}, reason={}", orderId, reason);

        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found for orderId=" + orderId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot fail payment: current status is " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);

        var evt = new PaymentFailedEvent(
                UUID.randomUUID(), orderId, reason, OffsetDateTime.now());
        eventPublisher.publish(PaymentTopics.PAYMENT_FAILED, evt);
        meterRegistry.counter("payment.failed", "service", "payment-service").increment();
        log.info("Payment failed for orderId={}, reason={}", orderId, reason);
    }

    @Transactional(readOnly = true)
    public PaymentDTO findByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found for orderId=" + orderId));
        return PaymentMapper.toDto(payment);
    }

    private void completePayment(Payment payment, BigDecimal amount) {
        String paymentReference = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaymentReference(paymentReference);
        paymentRepository.save(payment);

        var evt = new PaymentCompletedEvent(
                UUID.randomUUID(), payment.getOrderId(), paymentReference,
                amount, OffsetDateTime.now());
        eventPublisher.publish(PaymentTopics.PAYMENT_COMPLETED, evt);
        meterRegistry.counter("payment.completed", "service", "payment-service").increment();
        log.info("Payment completed for orderId={}, ref={}", payment.getOrderId(), paymentReference);
    }

    private void publishRefundFailed(UUID orderId, String reason) {
        var evt = new RefundFailedEvent(UUID.randomUUID(), orderId, reason, OffsetDateTime.now());
        eventPublisher.publish(PaymentTopics.REFUND_FAILED, evt);
        meterRegistry.counter("payment.refund.failed", "service", "payment-service").increment();
    }
}
