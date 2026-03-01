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

    public static Specification<Order> userIdEquals(String userId) {
        return (root, q, cb) -> userId == null ? cb.conjunction() : cb.equal(root.get("userId"), userId);
    }

    public static Specification<Order> minAmount(BigDecimal min) {
       return  (root, q, cb) -> min == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("totalAmount"), min);
    }

    public static Specification<Order> maxAmount(BigDecimal max) {
        return  (root, q, cb) -> max == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("totalAmount"), max);
    }

    public static Specification<Order> createdBetween(OffsetDateTime from, OffsetDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            } else if (from == null) {
                return cb.lessThanOrEqualTo(root.get("createdAt"), to);
            } else if (to == null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            } else {
                return cb.between(root.get("createdAt"), from, to);
            }
        };
    }
}
