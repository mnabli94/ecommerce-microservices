package com.demo.order.service;

import com.demo.order.dto.in.OrderInDTO;
import com.demo.order.dto.out.OrderItemOutDTO;
import com.demo.order.dto.out.OrderOutDTO;
import com.demo.order.entity.Order;
import com.demo.order.entity.OrderStatus;
import com.demo.order.mapper.OrderMapper;
import com.demo.order.repository.OrderRepository;
import com.demo.product.dto.ProductDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository,
                        OrderMapper orderMapper,
                        ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.productClient = productClient;
    }

    @Transactional
    public OrderOutDTO createOrder(OrderInDTO dto) {

        Order order = orderMapper.toEntity(dto);
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        order.getOrderItems().forEach(item -> {

            ProductDTO product = productClient.getProduct(Long.parseLong(item.getProductId()));
            if (product == null) {
                throw new EntityNotFoundException("Product not found: " + item.getProductId());
            }

            if (Boolean.FALSE.equals(product.inStock())) {
                throw new RuntimeException("Product not available: " + item.getProductId());
            }

            if (product.price() == null) {
                throw new RuntimeException("Product price missing: " + item.getProductId());
            }

            item.setUnitPrice(product.price());
            item.setOrder(order);
        });

        order.calculateTotalAmount();
        Order saved = orderRepository.save(order);

        OrderOutDTO result = orderMapper.toOutDto(saved);

        var finalOrderItems = saved.getOrderItems().stream().map(item -> {
            var pid = Long.valueOf(item.getProductId());
            var product = productClient.getProduct(pid);
            var itemDto = orderMapper.toOutDto(item);
            return new OrderItemOutDTO(itemDto.id(), product, itemDto.quantity(), itemDto.unitPrice());
        }).toList();

        return new OrderOutDTO(result.id(), result.status(), result.shippingAddress(), finalOrderItems, result.totalAmount());

    }
}
