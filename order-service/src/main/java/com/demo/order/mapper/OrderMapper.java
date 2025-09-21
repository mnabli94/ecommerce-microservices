package com.demo.order.mapper;

import com.demo.order.dto.in.OrderInDTO;
import com.demo.order.dto.in.OrderItemInDTO;
import com.demo.order.dto.out.OrderItemOutDTO;
import com.demo.order.dto.out.OrderOutDTO;
import com.demo.order.entity.Order;
import com.demo.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(OrderInDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    OrderItem toEntity(OrderItemInDTO dto);

    OrderItemInDTO toDto(OrderItem orderItem);

    OrderInDTO toDto(Order order);

    @Mapping(target = "product", ignore = true)
    OrderItemOutDTO toOutDto(OrderItem orderItem);

    OrderOutDTO toOutDto(Order order);
}
