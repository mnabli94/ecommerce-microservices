package com.demo.order.repository;

import com.demo.order.entity.Order;
import com.demo.order.entity.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class OrderSpecifications {
    public static Specification<Order> statusEquals(OrderStatus status) {
        return (root, q, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> minAmount(BigDecimal min) {
       return  (root, q, cb) -> min == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("totalAmount"), min);
    }

    public static Specification<Order> maxAmount(BigDecimal max) {
        return  (root, q, cb) -> max == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("totalAmount"), max);
    }

    public static Specification<Order> createdFrom(OffsetDateTime from) {
        return (root, q, cb) -> from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }
    public static Specification<Order> createdTo(OffsetDateTime to) {
        return (root, q, cb) -> to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
