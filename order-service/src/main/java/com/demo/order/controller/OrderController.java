package com.demo.order.controller;

import com.demo.order.dto.in.OrderInDTO;
import com.demo.order.dto.out.OrderOutDTO;
import com.demo.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderOutDTO> createOrder(@Valid @RequestBody OrderInDTO dto) {
        return ResponseEntity.ok(orderService.createOrder(dto));
    }
}
