package com.demo.order.controller;

import com.demo.order.dto.ShippingAddressDTO;
import com.demo.order.dto.in.CancelRequestDTO;
import com.demo.order.dto.in.OrderInDTO;
import com.demo.order.dto.in.OrderItemInDTO;
import com.demo.order.dto.out.OrderOutDTO;
import com.demo.order.entity.OrderStatus;
import com.demo.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.demo.order.service.OrderSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
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

    @MockBean
    private OrderSecurityService orderSecurityService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private final String CONTACT_EMAIL = "test@example.com";
    private final String PAYMENT_METHOD_ID = "pm_test_123";
    private final String PAYMENT_REFERENCE = "PAY_REF_123";
    private final String CANCELLATION_REASON = "OTHER";
    private final List<OrderItemInDTO> ITEMS = List.of(new OrderItemInDTO("1", 1, BigDecimal.TEN));
    private OrderInDTO orderInDTO;

    @BeforeEach
     void setUp() {
        orderInDTO = buildOrderInDTO();
    }

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
        UUID id = UUID.randomUUID();
        when(orderService.createOrder(any(), anyString())).thenReturn(buildOutDTO(id));

        mockMvc.perform(post("/api/orders")
                .principal(() -> "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderInDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shippingAddress.firstName").value("Alice"))
                .andExpect(jsonPath("$.shippingAddress.city").value("Paris"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createOrder_shouldReturnBadRequest_whenAddressHasMissingFields() throws Exception {
        var incompleteAddress = new ShippingAddressDTO(
                "", "Dupont", null, "12 rue de la Paix", "Paris", "75001", "FR"); // firstName blank
        var dto = new OrderInDTO(OrderStatus.PENDING, incompleteAddress, ITEMS, BigDecimal.TEN, CONTACT_EMAIL, PAYMENT_METHOD_ID);

        mockMvc.perform(post("/api/orders")
                .principal(() -> "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(incompleteAddress)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createOrder_shouldReturnBadRequest_whenNoItems() throws Exception {
        OrderInDTO noItems = new OrderInDTO(
                OrderStatus.PENDING,
                validAddress(),
                List.of(), null, CONTACT_EMAIL, PAYMENT_METHOD_ID);

        mockMvc.perform(post("/api/orders")
                .principal(() -> "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(noItems)))
                .andExpect(status().isBadRequest());
    }

    // PATCH /api/orders/{id}/cancel

    @Test
    @WithMockUser(roles = "USER")
    void cancel_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        var reason = "Customer changed their mind";
        var body = new CancelRequestDTO(reason);
        when(orderService.requestCancellation(any(), anyString()))
                .thenReturn(buildOutDTO(id, OrderStatus.CANCELLATION_REQUESTED, reason));

        mockMvc.perform(patch("/api/orders/{id}/cancel", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLATION_REQUESTED"))
                .andExpect(jsonPath("$.cancellationReason").value(reason));
    }

    @Test
    @WithMockUser(roles = "USER")
    void cancel_shouldReturnBadRequest_whenReasonIsBlank() throws Exception {
        UUID id = UUID.randomUUID();
        var body = new CancelRequestDTO("");

        mockMvc.perform(patch("/api/orders/{id}/cancel", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    private OrderInDTO buildOrderInDTO() {
        return new OrderInDTO(OrderStatus.PENDING, validAddress(),
                ITEMS,null, CONTACT_EMAIL, PAYMENT_METHOD_ID);}

    private ShippingAddressDTO validAddress() {
        return new ShippingAddressDTO(
                "Alice", "Dupont", "+33600000000",
                "12 rue de la Paix", "Paris", "75001", "FR");
    }

    private OrderOutDTO buildOutDTO(UUID id) {
        return buildOutDTO(id, OrderStatus.PENDING);
    }

    private OrderOutDTO buildOutDTO(UUID id, OrderStatus status) {
        return buildOutDTO(id, status, CANCELLATION_REASON);
    }

    private OrderOutDTO buildOutDTO(UUID id, OrderStatus status, String cancellationReason) {
        return new OrderOutDTO(
                id,
                "alice",
                CONTACT_EMAIL,
                status,
                validAddress(),
                List.of(),
                BigDecimal.valueOf(19.98),
                PAYMENT_REFERENCE,
                cancellationReason,
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}