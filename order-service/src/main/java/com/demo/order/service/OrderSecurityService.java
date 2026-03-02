package com.demo.order.service;

import com.demo.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("orderSecurity")
@RequiredArgsConstructor
public class OrderSecurityService {

    private final OrderRepository orderRepository;

    public boolean isOwner(UUID orderId, String username) {
        return orderRepository.existsByIdAndUserId(orderId, username);
    }
}
