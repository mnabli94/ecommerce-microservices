package com.demo.order.controller;

import com.demo.order.dto.out.OrderOutDTO;
import com.demo.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import java.util.UUID;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    @WithMockUser(roles = "USER")
    void getOrder_withValidToken_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        OrderOutDTO order = new OrderOutDTO(id, null, null, null, null, null);
        when(orderService.find(id)).thenReturn(order);
        mockMvc.perform(get("/api/orders/%s".formatted(id))).andExpect(status().isOk());
    }

    @Test
    void getOrder_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/orders/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}