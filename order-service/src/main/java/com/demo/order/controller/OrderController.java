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
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Validated
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "Endpoints for managing orders")
public class OrderController {
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "userId", "status", "totalAmount", "createdAt", "updatedAt");

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Get order by ID", description = "Retrieves an order by its unique identifier")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @GetMapping("/{id}")
    public ResponseEntity<OrderOutDTO> get(@NotNull @PathVariable UUID id) {
        return ResponseEntity.ok(orderService.find(id));
    }

    @Operation(summary = "List orders", description = "Retrieves a paginated list of orders with optional filtering")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping()
    public ResponseEntity<Page<OrderOutDTO>> get(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") @Pattern(regexp = "asc|desc") String direction,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to) {
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field: " + sortBy + ". Allowed: " + SORTABLE_FIELDS);
        }
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(orderService.findAll(status, minAmount, from, to, pageable));
    }

    @Operation(summary = "Create order", description = "Creates a new order")
    @ApiResponse(responseCode = "201", description = "Order created successfully")
    @PostMapping
    public ResponseEntity<OrderOutDTO> createOrder(@Valid @RequestBody OrderInDTO dto, Principal principal) {
        var out = orderService.createOrder(dto, principal.getName());
        return ResponseEntity
                .created(URI.create("/orders" + out.id()))
                .body(out);
    }

    @Operation(summary = "Confirm order", description = "Confirms an existing order")
    @ApiResponse(responseCode = "200", description = "Order confirmed successfully")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @PatchMapping("/{id}/confirm")
    public OrderOutDTO confirm(@PathVariable UUID id) {
        return orderService.confirm(id);
    }

    @Operation(summary = "Cancel order", description = "Cancels an existing order")
    @ApiResponse(responseCode = "200", description = "Order cancelled successfully")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @PatchMapping("/{id}/cancel")
    public OrderOutDTO cancel(@PathVariable UUID id) {
        return orderService.cancel(id);
    }
}
