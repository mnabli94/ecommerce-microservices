package com.demo.order.repository;

import com.demo.order.entity.Order;
import com.demo.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    boolean existsByIdAndUserId(UUID id, String userId);

    List<Order> findByStatusInAndExpiresAtBefore(List<OrderStatus> statuses, OffsetDateTime threshold);
}
