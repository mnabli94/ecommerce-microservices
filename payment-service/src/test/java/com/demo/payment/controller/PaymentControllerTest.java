package com.demo.payment.controller;

import com.demo.payment.dto.PaymentDTO;
import com.demo.payment.entity.PaymentStatus;
import com.demo.payment.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @WithMockUser
    void getByOrderId_shouldReturnPayment() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentDTO dto = new PaymentDTO(
                UUID.randomUUID(), orderId, "user-1", BigDecimal.valueOf(99.99),
                PaymentStatus.COMPLETED, "PAY-abc12345", null, OffsetDateTime.now());

        when(paymentService.findByOrderId(orderId)).thenReturn(dto);

        mockMvc.perform(get("/api/payments").param("orderId", orderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.paymentReference").value("PAY-abc12345"));
    }

    @Test
    @WithMockUser
    void getByOrderId_shouldReturn404_whenNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(paymentService.findByOrderId(orderId))
                .thenThrow(new EntityNotFoundException("Payment not found for orderId=" + orderId));

        mockMvc.perform(get("/api/payments").param("orderId", orderId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
