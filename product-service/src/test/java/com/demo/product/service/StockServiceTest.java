package com.demo.product.service;

import com.demo.events.order.Item;
import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.stock.StockTopics;
import com.demo.kafka.utils.producer.EventPublisher;
import com.demo.product.entity.Product;
import com.demo.product.entity.ReservedItem;
import com.demo.product.entity.StockReservation;
import com.demo.product.repository.ProductRepository;
import com.demo.product.repository.StockReservationRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
class StockServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private StockReservationRepository stockReservationRepository;
    @Mock
    private EventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<StockReservation> reservationCaptor;

    private StockService stockService;

    @BeforeEach
    void setUp() {
        stockService = new StockService(
                productRepository, stockReservationRepository,
                eventPublisher, new SimpleMeterRegistry());
    }

    private static Product product(Long id, int quantity, int reserved) {
        Product p = new Product();
        p.setId(id);
        p.setName("Product " + id);
        p.setPrice(BigDecimal.TEN);
        p.setQuantity(quantity);
        p.setReservedQuantity(reserved);
        p.updateInStockStatus();
        return p;
    }

    private static OrderCreatedEvent orderEvent(UUID orderId, List<Item> items) {
        return new OrderCreatedEvent(
                UUID.randomUUID(), orderId, "user-1", BigDecimal.valueOf(100),
                "user@test.com", "pm-1", OffsetDateTime.now(), items);
    }

    @Test
    void reserveStock_shouldReserveAllItems_whenStockAvailable() {
        UUID orderId = UUID.randomUUID();
        var items = List.of(
                new Item(1L, 3, BigDecimal.TEN),
                new Item(2L, 5, BigDecimal.valueOf(20)));

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product(1L, 10, 0)));
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(product(2L, 10, 0)));

        stockService.reserveStock(orderEvent(orderId, items));

        verify(productRepository, times(2)).save(any(Product.class));
        verify(stockReservationRepository).save(reservationCaptor.capture());
        verify(eventPublisher).publish(eq(StockTopics.STOCK_RESERVED), any());

        StockReservation saved = reservationCaptor.getValue();
        assertEquals(orderId, saved.getOrderId());
        assertEquals(2, saved.getItems().size());
    }

    @Test
    void reserveStock_shouldPublishFailed_whenProductNotFound() {
        UUID orderId = UUID.randomUUID();
        var items = List.of(new Item(99L, 1, BigDecimal.TEN));

        when(productRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        stockService.reserveStock(orderEvent(orderId, items));

        verify(eventPublisher).publish(eq(StockTopics.STOCK_RESERVATION_FAILED), any());
        verify(productRepository, never()).save(any());
        verify(stockReservationRepository, never()).save(any());
    }

    @Test
    void reserveStock_shouldPublishFailed_whenInsufficientStock() {
        UUID orderId = UUID.randomUUID();
        var items = List.of(new Item(1L, 15, BigDecimal.TEN));

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product(1L, 10, 0)));

        stockService.reserveStock(orderEvent(orderId, items));

        verify(eventPublisher).publish(eq(StockTopics.STOCK_RESERVATION_FAILED), any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void reserveStock_shouldAggregateDuplicateProducts() {
        UUID orderId = UUID.randomUUID();
        // Même produit deux fois : demande totale = 3 + 8 = 11, mais available = 10
        var items = List.of(
                new Item(1L, 3, BigDecimal.TEN),
                new Item(1L, 8, BigDecimal.TEN));

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product(1L, 10, 0)));

        stockService.reserveStock(orderEvent(orderId, items));

        // Doit échouer car la demande agrégée (11) > disponible (10)
        verify(eventPublisher).publish(eq(StockTopics.STOCK_RESERVATION_FAILED), any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void releaseStock_shouldDecrementReservedQuantity() {
        UUID orderId = UUID.randomUUID();
        var reservation = new StockReservation(orderId,
                List.of(new ReservedItem(1L, 5)), OffsetDateTime.now());
        Product product = product(1L, 10, 5);

        when(stockReservationRepository.findById(orderId)).thenReturn(Optional.of(reservation));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        stockService.releaseStock(orderId);

        assertEquals(0, product.getReservedQuantity());
        assertTrue(product.isInStock());
        verify(productRepository).save(product);
        verify(stockReservationRepository).delete(reservation);
    }

    @Test
    void releaseStock_shouldDoNothing_whenNoReservationFound() {
        UUID orderId = UUID.randomUUID();
        when(stockReservationRepository.findById(orderId)).thenReturn(Optional.empty());

        stockService.releaseStock(orderId);

        verify(productRepository, never()).save(any());
        verify(stockReservationRepository, never()).delete(any());
    }
}
