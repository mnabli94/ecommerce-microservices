package com.demo.product.service;

import com.demo.events.order.Item;
import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.stock.StockReservationFailedEvent;
import com.demo.events.stock.StockReservedEvent;
import com.demo.events.stock.StockTopics;
import com.demo.kafka.utils.producer.EventPublisher;
import com.demo.product.entity.Product;
import com.demo.product.entity.ReservedItem;
import com.demo.product.entity.StockReservation;
import com.demo.product.repository.ProductRepository;
import com.demo.product.repository.StockReservationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class StockService {

    private final ProductRepository productRepository;
    private final StockReservationRepository stockReservationRepository;
    private final EventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    public StockService(ProductRepository productRepository,
                        StockReservationRepository stockReservationRepository,
                        EventPublisher eventPublisher,
                        MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.stockReservationRepository = stockReservationRepository;
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Tente de réserver le stock pour tous les items de la commande.
     * Si tous les items sont disponibles → réserve + publie stock.reserved.
     * Si au moins un item est insuffisant → publie stock.reservation.failed (sans modifier le stock).
     * Doit être appelé dans une transaction existante (@Transactional du listener).
     */
    public void reserveStock(OrderCreatedEvent event) {
        log.info("Attempting stock reservation for orderId={}", event.orderId());

        // Étape 1 : verrouiller et vérifier tous les produits
        Map<Long, Product> lockedProducts = new LinkedHashMap<>();
        for (Item item : event.items()) {
            Optional<Product> opt = productRepository.findByIdForUpdate(item.productId());
            if (opt.isEmpty()) {
                String reason = "Product not found: id=%d".formatted(item.productId());
                log.warn("Stock reservation failed — {}", reason);
                publishFailed(event.orderId(), reason);
                return;
            }
            Product product = opt.get();

            if (product.getAvailableQuantity() < item.quantity()) {
                String reason = "Insufficient stock for product %d: available=%d, requested=%d"
                        .formatted(item.productId(), product.getAvailableQuantity(), item.quantity());
                log.warn("Stock reservation failed — {}", reason);
                publishFailed(event.orderId(), reason);
                return;
            }
            lockedProducts.put(item.productId(), product);
        }

        List<ReservedItem> reservedItems = new ArrayList<>();
        for (Item item : event.items()) {
            Product product = lockedProducts.get(item.productId());
            product.setReservedQuantity(product.getReservedQuantity() + item.quantity());
            product.updateInStockStatus();
            productRepository.save(product);
            reservedItems.add(new ReservedItem(item.productId(), item.quantity()));
        }

        stockReservationRepository.save(
                new StockReservation(event.orderId(), reservedItems, OffsetDateTime.now()));

        var evt = new StockReservedEvent(
                UUID.randomUUID(),
                event.orderId(),
                event.userId(),
                event.contactEmail(),
                event.paymentMethodId(),
                event.totalAmount(),
                event.items(),
                OffsetDateTime.now());

        eventPublisher.publish(StockTopics.STOCK_RESERVED, evt);
        meterRegistry.counter("stock.reserved", "service", "product-service").increment();
        log.info("Stock reserved successfully for orderId={}", event.orderId());
    }

    /**
     * Libère le stock réservé pour une commande annulée.
     * Doit être appelé dans une transaction existante (@Transactional du listener).
     */
    public void releaseStock(UUID orderId) {
        log.info("Releasing stock reservation for orderId={}", orderId);

        stockReservationRepository.findById(orderId).ifPresentOrElse(reservation -> {
            for (ReservedItem reservedItem : reservation.getItems()) {
                productRepository.findByIdForUpdate(reservedItem.getProductId()).ifPresentOrElse(product -> {
                    int newReserved = Math.max(0, product.getReservedQuantity() - reservedItem.getQuantity());
                    product.setReservedQuantity(newReserved);
                    product.updateInStockStatus();
                    productRepository.save(product);
                }, () -> log.warn("Product {} not found during stock release for orderId={}", reservedItem.getProductId(), orderId));
            }
            stockReservationRepository.delete(reservation);
            meterRegistry.counter("stock.released", "service", "product-service").increment();
            log.info("Stock released for orderId={}", orderId);
        }, () -> log.warn("No stock reservation found for orderId={} (already released or never reserved)", orderId));
    }

    private void publishFailed(UUID orderId, String reason) {
        var evt = new StockReservationFailedEvent(UUID.randomUUID(), orderId, reason, OffsetDateTime.now());
        eventPublisher.publish(StockTopics.STOCK_RESERVATION_FAILED, evt);
        meterRegistry.counter("stock.reservation.failed", "service", "product-service").increment();
    }
}
