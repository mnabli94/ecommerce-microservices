package com.demo.order.controller;

import com.demo.order.dto.in.OrderInDTO;
import com.demo.order.dto.out.OrderOutDTO;
import com.demo.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderOutDTO> get(@NotNull @PathVariable UUID id){
        return ResponseEntity.ok(orderService.find(id));
    }

    @GetMapping()
    public ResponseEntity<Page<OrderOutDTO>> get(@RequestParam(name = "page", defaultValue = "0") int page,
                                                 @RequestParam(name = "size", defaultValue = "10") int size,
                                                 @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
                                                 @RequestParam(name = "direction", defaultValue = "asc") String direction){
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page,size).withSort(sort);
        return ResponseEntity.ok(orderService.findAll(pageable));
    }

    @PostMapping
    public ResponseEntity<OrderOutDTO> createOrder(@Valid @RequestBody OrderInDTO dto) {
        var out = orderService.createOrder(dto);
        return ResponseEntity
                .created(URI.create("/orders" + out.id()))
                .body(out);
    }

    @PatchMapping("/{id}/confirm")
    public OrderOutDTO confirm(@PathVariable UUID id) { return orderService.confirm(id); }

    @PatchMapping("/{id}/cancel")
    public OrderOutDTO cancel(@PathVariable UUID id) { return orderService.cancel(id); }
}
