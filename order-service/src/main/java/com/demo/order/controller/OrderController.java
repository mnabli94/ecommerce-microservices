package com.demo.order.controller;

import com.demo.order.dto.in.OrderInDTO;
import com.demo.order.dto.out.OrderOutDTO;
import com.demo.order.entity.OrderStatus;
import com.demo.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderOutDTO> get(@NotNull @PathVariable UUID id) {
        return ResponseEntity.ok(orderService.find(id));
    }

    @GetMapping()
    public ResponseEntity<Page<OrderOutDTO>> get(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
                                                 @RequestParam(defaultValue = "createdAt") String sortBy,
                                                 @RequestParam(defaultValue = "asc") @Pattern(regexp = "asc|desc") String direction,
                                                 @RequestParam(required = false) OrderStatus status,
                                                 @RequestParam(required = false) BigDecimal minAmount,
                                                 @RequestParam(required = false) OffsetDateTime from,
                                                 @RequestParam(required = false) OffsetDateTime to) {
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(orderService.findAll(status, minAmount, from, to, pageable));
    }

    @PostMapping
    public ResponseEntity<OrderOutDTO> createOrder(@Valid @RequestBody OrderInDTO dto) {
        var out = orderService.createOrder(dto);
        return ResponseEntity
                .created(URI.create("/orders" + out.id()))
                .body(out);
    }

    @PatchMapping("/{id}/confirm")
    public OrderOutDTO confirm(@PathVariable UUID id) {
        return orderService.confirm(id);
    }

    @PatchMapping("/{id}/cancel")
    public OrderOutDTO cancel(@PathVariable UUID id) {
        return orderService.cancel(id);
    }
}
