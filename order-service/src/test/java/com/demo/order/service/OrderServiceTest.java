package com.demo.order.service;

import com.demo.events.order.OrderTopics;
import com.demo.events.payment.PaymentCompletedEvent;
import com.demo.kafka.utils.producer.EventPublisher;
import com.demo.order.dto.ShippingAddressDTO;
import com.demo.order.dto.in.OrderInDTO;
import com.demo.order.dto.in.OrderItemInDTO;
import com.demo.order.dto.out.OrderOutDTO;
import com.demo.order.dto.out.ProductDTO;
import com.demo.order.entity.Order;
import com.demo.order.entity.OrderItem;
import com.demo.order.entity.OrderStatus;
import com.demo.order.mapper.OrderMapper;
import com.demo.order.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ProductCaller productCaller;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private Counter counter;

    @InjectMocks
    private OrderService orderService;

    private final String USERNAME = "testUser";
    private final String CONTACT_EMAIL = "test@example.com";
    private final String PAYMENT_METHOD_ID = "pm_test_123";
    private final UUID ORDER_ID = UUID.randomUUID();
    private final String PAYMENT_REFERENCE = "PAY_REF_123";
    private final PaymentCompletedEvent paymentCompletedEvent = new PaymentCompletedEvent(
            UUID.randomUUID(),
            ORDER_ID,
            PAYMENT_REFERENCE,
            BigDecimal.TEN,
            OffsetDateTime.now()
    );


    @BeforeEach
    void setUp() {
        lenient().when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
    }

    @Test
    void createOrder_shouldSucceed_whenDataIsValid() {
        // Given
        OrderItemInDTO itemIn = new OrderItemInDTO("101", 2, new BigDecimal("50.00"));
        OrderInDTO orderIn = new OrderInDTO(
                OrderStatus.PENDING, mock(ShippingAddressDTO.class), List.of(itemIn), null,
                CONTACT_EMAIL, PAYMENT_METHOD_ID);

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(USERNAME);
        order.setContactEmail(CONTACT_EMAIL);
        order.setPaymentMethodId(PAYMENT_METHOD_ID);
        OrderItem entityItem = new OrderItem();
        entityItem.setProductId("101");
        entityItem.setQuantity(2);
        order.setOrderItems(List.of(entityItem));

        ProductDTO product = new ProductDTO(101L, "Test Product", new BigDecimal("50.00"), true, 1L);
        OrderOutDTO orderOut = new OrderOutDTO(
                ORDER_ID, USERNAME, CONTACT_EMAIL, OrderStatus.PENDING,
                null, List.of(), new BigDecimal("100.00"),
                null, null, OffsetDateTime.now(), null, null);

        when(orderMapper.toEntity(any(OrderInDTO.class))).thenReturn(order);
        when(orderMapper.toEntity(any(OrderItemInDTO.class))).thenReturn(entityItem);
        when(productCaller.getProduct(101L)).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toOutDto(any(Order.class))).thenReturn(orderOut);

        // When
        OrderOutDTO result = orderService.createOrder(orderIn, USERNAME);

        // Then
        assertNotNull(result);
        assertEquals(ORDER_ID, result.id());
        assertEquals(CONTACT_EMAIL, result.contactEmail());
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publish(eq(OrderTopics.ORDER_CREATED), any());
    }

    @Test
    void createOrder_shouldThrowException_whenProductNotFound() {
        // Given
        OrderItemInDTO itemIn = new OrderItemInDTO("101", 1, new BigDecimal("50.00"));
        OrderInDTO orderIn = new OrderInDTO(
                OrderStatus.PENDING, null, List.of(itemIn), null,
                CONTACT_EMAIL, PAYMENT_METHOD_ID);

        Order order = new Order();
        order.setOrderItems(List.of(new OrderItem()));
        order.getOrderItems().get(0).setProductId("101");

        OrderItem item = new OrderItem();
        item.setProductId("101");

        when(orderMapper.toEntity(any(OrderInDTO.class))).thenReturn(order);
        when(orderMapper.toEntity(any(OrderItemInDTO.class))).thenReturn(item);
        when(productCaller.getProduct(101L)).thenReturn(null);

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> orderService.createOrder(orderIn, USERNAME));
    }

    @Test
    void createOrder_shouldThrowException_whenProductOutOfStock() {
        // Given
        OrderItemInDTO itemIn = new OrderItemInDTO("101", 1, new BigDecimal("50.00"));
        OrderInDTO orderIn = new OrderInDTO(
                OrderStatus.PENDING, null, List.of(itemIn), null,
                CONTACT_EMAIL, PAYMENT_METHOD_ID);

        Order order = new Order();
        OrderItem item = new OrderItem();
        item.setProductId("101");
        order.setOrderItems(List.of(item));

        ProductDTO product = new ProductDTO(101L, "Test Product", new BigDecimal("50.00"), false, 1L);

        when(orderMapper.toEntity(any(OrderInDTO.class))).thenReturn(order);
        when(orderMapper.toEntity(any(OrderItemInDTO.class))).thenReturn(item);
        when(productCaller.getProduct(101L)).thenReturn(product);

        // When & Then
        assertThrows(IllegalStateException.class, () -> orderService.createOrder(orderIn, USERNAME));
    }

    @Test
    void find_shouldReturnOrder_whenIdExists() {
        // Given
        Order order = new Order();
        order.setId(ORDER_ID);
        OrderOutDTO orderOut = new OrderOutDTO(
                ORDER_ID, USERNAME, CONTACT_EMAIL, OrderStatus.PENDING,
                null, List.of(), BigDecimal.TEN,
                null, null, OffsetDateTime.now(), null, null);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderMapper.toOutDto(order)).thenReturn(orderOut);

        // When
        OrderOutDTO result = orderService.find(ORDER_ID);

        // Then
        assertEquals(ORDER_ID, result.id());
    }

    @Test
    void confirm_shouldUpdateStatus_whenPending() {
        // Given
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderItems(List.of());

        OrderOutDTO orderOut = new OrderOutDTO(
                ORDER_ID, USERNAME, CONTACT_EMAIL, OrderStatus.CONFIRMED,
                null, List.of(), BigDecimal.TEN,
                null, null, OffsetDateTime.now(), null, null);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toOutDto(order)).thenReturn(orderOut);

        // When
        OrderOutDTO result = orderService.confirm(paymentCompletedEvent);

        // Then
        assertEquals(OrderStatus.CONFIRMED, result.status());
        verify(eventPublisher).publish(eq(OrderTopics.ORDER_CONFIRMED), any());
    }

    @Test
    void confirm_shouldThrowException_whenNotPending() {
        // Given
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        // When & Then
        assertThrows(IllegalStateException.class, () -> orderService.confirm(paymentCompletedEvent));
    }

    @Test
    void cancel_shouldUpdateStatus_whenNotConfirmed() {
        // Given
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.PENDING);

        OrderOutDTO orderOut = new OrderOutDTO(
                ORDER_ID, USERNAME, CONTACT_EMAIL, OrderStatus.CANCELLED,
                null, List.of(), BigDecimal.TEN,
                null, null, OffsetDateTime.now(), null, null);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toOutDto(order)).thenReturn(orderOut);

        // When
        OrderOutDTO result = orderService.cancel(ORDER_ID, "Cancelled by user");

        // Then
        assertEquals(OrderStatus.CANCELLED, result.status());
        verify(eventPublisher).publish(eq(OrderTopics.ORDER_CANCELLED), any());
    }

    @Test
    void cancel_shouldThrowException_whenAlreadyConfirmed() {
        // Given
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        // When & Then
        assertThrows(IllegalStateException.class, () -> orderService.cancel(ORDER_ID, "Cancelled by user"));
    }

    @Test
    void requestCancellation_shouldSetCancellationRequested_whenConfirmed() {
        // Given
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.CONFIRMED);

        OrderOutDTO orderOut = new OrderOutDTO(
                ORDER_ID, USERNAME, CONTACT_EMAIL, OrderStatus.CANCELLATION_REQUESTED,
                null, List.of(), BigDecimal.TEN,
                null, "Customer changed their mind", OffsetDateTime.now(), null, null);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toOutDto(order)).thenReturn(orderOut);

        // When
        OrderOutDTO result = orderService.requestCancellation(ORDER_ID, "Customer changed their mind");

        // Then
        assertEquals(OrderStatus.CANCELLATION_REQUESTED, result.status());
        verify(eventPublisher).publish(eq(OrderTopics.ORDER_CANCELLATION_REQUESTED), any());
    }

    @Test
    void requestCancellation_shouldThrowException_whenPending() {
        // Given
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        // When & Then
        assertThrows(IllegalStateException.class,
                () -> orderService.requestCancellation(ORDER_ID, "Customer changed their mind"));
    }
}
