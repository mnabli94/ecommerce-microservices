package com.demo.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stock_reservation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @ElementCollection
    @CollectionTable(
            name = "stock_reservation_item",
            joinColumns = @JoinColumn(name = "order_id")
    )
    private List<ReservedItem> items = new ArrayList<>();

    @Column(name = "reserved_at", nullable = false)
    private OffsetDateTime reservedAt;
}
