package com.demo.order.controller;

import com.demo.order.dto.ShippingAddressDTO;
import com.demo.order.dto.in.OrderInDTO;
import com.demo.order.dto.in.OrderItemInDTO;
import com.demo.order.dto.out.OrderOutDTO;
import com.demo.order.entity.OrderStatus;
import com.demo.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    // GET /api/orders/{id}
    @Test
    @WithMockUser(roles = "USER")
    void getOrder_shouldReturnOk_whenExists() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.find(id)).thenReturn(buildOutDTO(id));
        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.shippingAddress.city").value("Paris"))
                .andExpect(jsonPath("$.shippingAddress.country").value("FR"));
        ;
    }

    @Disabled
    @Test
    void getOrder_shouldReturnUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // POST /api/orders ─────────────

    @Test
    @WithMockUser(roles = "USER")
    void createOrder_shouldReturnCreated_withStructuredAddress() throws Exception {
        var dto = buildOrderInDTO();
        UUID id = UUID.randomUUID();
        when(orderService.createOrder(any(), anyString())).thenReturn(buildOutDTO(id));

        mockMvc.perform(post("/api/orders")
                .principal(() -> "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shippingAddress.firstName").value("Alice"))
                .andExpect(jsonPath("$.shippingAddress.city").value("Paris"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createOrder_shouldReturnBadRequest_whenAddressHasMissingFields() throws Exception {
        var incompleteAddress = new ShippingAddressDTO(
                "", "Dupont", null, "12 rue de la Paix", "Paris", "75001", "FR"); // firstName blank
        var dto = new OrderInDTO(OrderStatus.PENDING, incompleteAddress,
                List.of(new OrderItemInDTO("1", 1, BigDecimal.TEN)), null);

        mockMvc.perform(post("/api/orders")
                .principal(() -> "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createOrder_shouldReturnBadRequest_whenNoItems() throws Exception {
        var dto = new OrderInDTO(OrderStatus.PENDING, validAddress(), List.of(), null);

        mockMvc.perform(post("/api/orders")
                .principal(() -> "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // PATCH /api/orders/{id}/confirm ───────────────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    void confirm_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.confirm(id)).thenReturn(buildOutDTO(id, OrderStatus.CONFIRMED));

        mockMvc.perform(patch("/api/orders/{id}/confirm", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    // PATCH /api/orders/{id}/cancel

    @Test
    @WithMockUser(roles = "USER")
    void cancel_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.cancel(id)).thenReturn(buildOutDTO(id, OrderStatus.CANCELLED));

        mockMvc.perform(patch("/api/orders/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private OrderInDTO buildOrderInDTO() {
        return new OrderInDTO(OrderStatus.PENDING, validAddress(),
                List.of(new OrderItemInDTO("1", 2, BigDecimal.valueOf(9.99))), null);
    }

    private ShippingAddressDTO validAddress() {
        return new ShippingAddressDTO(
                "Alice", "Dupont", "+33600000000",
                "12 rue de la Paix", "Paris", "75001", "FR");
    }

    private OrderOutDTO buildOutDTO(UUID id) {
        return buildOutDTO(id, OrderStatus.PENDING);
    }

    private OrderOutDTO buildOutDTO(UUID id, OrderStatus status) {
        return new OrderOutDTO(id, "alice", status, validAddress(),
                List.of(), BigDecimal.valueOf(19.98), OffsetDateTime.now());
    }
}