package com.demo.order.mapper;

import com.demo.order.dto.in.OrderInDTO;
import com.demo.order.dto.in.OrderItemInDTO;
import com.demo.order.dto.out.OrderItemOutDTO;
import com.demo.order.dto.out.OrderOutDTO;
import com.demo.order.entity.Order;
import com.demo.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    Order toEntity(OrderInDTO dto);
    @Mapping(target = "order", ignore = true)
    OrderItem toEntity(OrderItemInDTO dto);

    OrderItemInDTO toDto(OrderItem orderItem);
    OrderInDTO toDto(Order order);

    @Mapping(target = "product", ignore = true)
    OrderItemOutDTO toOutDto(OrderItem orderItem);
    OrderOutDTO toOutDto(Order order);
}
