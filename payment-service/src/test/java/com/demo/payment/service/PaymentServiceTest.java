package com.demo.payment.service;

import com.demo.events.order.Item;
import com.demo.events.payment.PaymentTopics;
import com.demo.events.stock.StockReservedEvent;
import com.demo.kafka.utils.producer.EventPublisher;
import com.demo.payment.dto.PaymentDTO;
import com.demo.payment.entity.Payment;
import com.demo.payment.entity.PaymentStatus;
import com.demo.payment.repository.PaymentRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private EventPublisher eventPublisher;
    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, eventPublisher, new SimpleMeterRegistry());
    }

    private static StockReservedEvent stockReservedEvent(UUID orderId, BigDecimal amount) {
        return new StockReservedEvent(
                UUID.randomUUID(), orderId, "user-1", "user@test.com", "pm-1",
                amount, List.of(new Item(1L, 2, BigDecimal.TEN)), OffsetDateTime.now());
    }

    private static Payment payment(UUID orderId, PaymentStatus status) {
        Payment p = new Payment();
        p.setId(UUID.randomUUID());
        p.setOrderId(orderId);
        p.setUserId("user-1");
        p.setAmount(BigDecimal.valueOf(100));
        p.setStatus(status);
        p.setPaymentReference("PAY-abc12345");
        p.setCreatedAt(OffsetDateTime.now());
        p.setUpdatedAt(OffsetDateTime.now());
        return p;
    }

    @Test
    void processPayment_shouldComplete() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processPayment(stockReservedEvent(orderId, BigDecimal.valueOf(50)));

        verify(paymentRepository, times(2)).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getAllValues().get(1); // second save = after completion
        assertEquals(PaymentStatus.COMPLETED, saved.getStatus());
        assertNotNull(saved.getPaymentReference());
        assertTrue(saved.getPaymentReference().startsWith("PAY-"));
        verify(eventPublisher).publish(eq(PaymentTopics.PAYMENT_COMPLETED), any());
    }

    @Test
    void processPayment_shouldSkipIfAlreadyExists() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment(orderId, PaymentStatus.COMPLETED)));

        paymentService.processPayment(stockReservedEvent(orderId, BigDecimal.valueOf(50)));

        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void processRefund_shouldRefundCompletedPayment() {
        UUID orderId = UUID.randomUUID();
        Payment existing = payment(orderId, PaymentStatus.COMPLETED);
        when(paymentRepository.findByOrderIdForUpdate(orderId)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processRefund(orderId, "Customer requested cancellation");

        assertEquals(PaymentStatus.REFUNDED, existing.getStatus());
        verify(eventPublisher).publish(eq(PaymentTopics.REFUND_COMPLETED), any());
    }

    @Test
    void processRefund_shouldFail_whenPaymentNotFound() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderIdForUpdate(orderId)).thenReturn(Optional.empty());

        paymentService.processRefund(orderId, "Customer requested cancellation");

        verify(eventPublisher).publish(eq(PaymentTopics.REFUND_FAILED), any());
    }

    @Test
    void processRefund_shouldFail_whenNotCompleted() {
        UUID orderId = UUID.randomUUID();
        Payment pending = payment(orderId, PaymentStatus.PENDING);
        when(paymentRepository.findByOrderIdForUpdate(orderId)).thenReturn(Optional.of(pending));

        paymentService.processRefund(orderId, "Customer requested cancellation");

        assertEquals(PaymentStatus.PENDING, pending.getStatus()); // unchanged
        verify(eventPublisher).publish(eq(PaymentTopics.REFUND_FAILED), any());
    }

    @Test
    void findByOrderId_shouldReturnPayment() {
        UUID orderId = UUID.randomUUID();
        Payment existing = payment(orderId, PaymentStatus.COMPLETED);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        PaymentDTO dto = paymentService.findByOrderId(orderId);

        assertEquals(orderId, dto.orderId());
        assertEquals(PaymentStatus.COMPLETED, dto.status());
        assertEquals("PAY-abc12345", dto.paymentReference());
    }

    @Test
    void findByOrderId_shouldThrow_whenNotFound() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> paymentService.findByOrderId(orderId));
    }

    // ── Webhook tests ────────────────────────────────────────────────

    @Test
    void completeFromWebhook_shouldComplete_whenPending() {
        UUID orderId = UUID.randomUUID();
        Payment pending = payment(orderId, PaymentStatus.PENDING);
        when(paymentRepository.findByOrderIdForUpdate(orderId)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.completeFromWebhook(orderId);

        assertEquals(PaymentStatus.COMPLETED, pending.getStatus());
        assertNotNull(pending.getPaymentReference());
        assertTrue(pending.getPaymentReference().startsWith("PAY-"));
        verify(eventPublisher).publish(eq(PaymentTopics.PAYMENT_COMPLETED), any());
    }

    @Test
    void completeFromWebhook_shouldReject_ifNotPending() {
        UUID orderId = UUID.randomUUID();
        Payment completed = payment(orderId, PaymentStatus.COMPLETED);
        when(paymentRepository.findByOrderIdForUpdate(orderId)).thenReturn(Optional.of(completed));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> paymentService.completeFromWebhook(orderId));

        assertTrue(ex.getMessage().contains("COMPLETED"));
        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void failFromWebhook_shouldFail_whenPending() {
        UUID orderId = UUID.randomUUID();
        Payment pending = payment(orderId, PaymentStatus.PENDING);
        when(paymentRepository.findByOrderIdForUpdate(orderId)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.failFromWebhook(orderId, "Insufficient funds");

        assertEquals(PaymentStatus.FAILED, pending.getStatus());
        assertEquals("Insufficient funds", pending.getFailureReason());
        verify(eventPublisher).publish(eq(PaymentTopics.PAYMENT_FAILED), any());
    }

    @Test
    void failFromWebhook_shouldReject_ifNotPending() {
        UUID orderId = UUID.randomUUID();
        Payment completed = payment(orderId, PaymentStatus.COMPLETED);
        when(paymentRepository.findByOrderIdForUpdate(orderId)).thenReturn(Optional.of(completed));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> paymentService.failFromWebhook(orderId, "Declined"));

        assertTrue(ex.getMessage().contains("COMPLETED"));
        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any(), any());
    }
}
