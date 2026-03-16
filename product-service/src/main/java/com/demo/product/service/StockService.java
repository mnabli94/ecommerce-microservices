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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserveStock(OrderCreatedEvent event) {
        log.info("Attempting stock reservation for orderId={}", event.orderId());

        Map<Long, Integer> demandByProduct = new LinkedHashMap<>();
        for (Item item : event.items()) {
            demandByProduct.merge(item.productId(), item.quantity(), Integer::sum);
        }

        Map<Long, Product> lockedProducts = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : demandByProduct.entrySet()) {
            Long productId = entry.getKey();
            int totalDemand = entry.getValue();

            Optional<Product> opt = productRepository.findByIdForUpdate(productId);
            if (opt.isEmpty()) {
                String reason = "Product not found: id=%d".formatted(productId);
                log.warn("Stock reservation failed — {}", reason);
                publishFailed(event.orderId(), reason);
                return;
            }
            Product product = opt.get();

            if (product.getAvailableQuantity() < totalDemand) {
                String reason = "Insufficient stock for product %d: available=%d, requested=%d"
                        .formatted(productId, product.getAvailableQuantity(), totalDemand);
                log.warn("Stock reservation failed — {}", reason);
                publishFailed(event.orderId(), reason);
                return;
            }
            lockedProducts.put(productId, product);
        }

        List<ReservedItem> reservedItems = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : demandByProduct.entrySet()) {
            Product product = lockedProducts.get(entry.getKey());
            product.setReservedQuantity(product.getReservedQuantity() + entry.getValue());
            product.updateInStockStatus();
            productRepository.save(product);
            reservedItems.add(new ReservedItem(entry.getKey(), entry.getValue()));
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
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseStock(UUID orderId) {
        log.info("Releasing stock reservation for orderId={}", orderId);

        Optional<StockReservation> optReservation = stockReservationRepository.findById(orderId);
        if (optReservation.isEmpty()) {
            log.warn("No stock reservation found for orderId={} (already released or never reserved)", orderId);
            return;
        }

        StockReservation reservation = optReservation.get();
        for (ReservedItem reservedItem : reservation.getItems()) {
            Optional<Product> optProduct = productRepository.findByIdForUpdate(reservedItem.getProductId());
            if (optProduct.isEmpty()) {
                log.warn("Product {} not found during stock release for orderId={}",
                        reservedItem.getProductId(), orderId);
                continue;
            }
            Product product = optProduct.get();
            int newReserved = Math.max(0, product.getReservedQuantity() - reservedItem.getQuantity());
            product.setReservedQuantity(newReserved);
            product.updateInStockStatus();
            productRepository.save(product);
        }

        stockReservationRepository.delete(reservation);
        meterRegistry.counter("stock.released", "service", "product-service").increment();
        log.info("Stock released for orderId={}", orderId);
    }

    private void publishFailed(UUID orderId, String reason) {
        var evt = new StockReservationFailedEvent(UUID.randomUUID(), orderId, reason, OffsetDateTime.now());
        eventPublisher.publish(StockTopics.STOCK_RESERVATION_FAILED, evt);
        meterRegistry.counter("stock.reservation.failed", "service", "product-service").increment();
    }
}
