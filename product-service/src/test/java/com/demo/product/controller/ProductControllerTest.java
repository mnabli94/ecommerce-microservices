package com.demo.product.controller;

import com.demo.product.dto.ProductDTO;
import com.demo.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ProductService service;

    @Test
    void create_shouldReturnSuccess() throws Exception {
        var dto = new ProductDTO(1L, "Laptop", new BigDecimal("19.90"), true, 10L);
        when(service.create(any())).thenReturn(dto);
        mvc.perform(
                post("/api/products").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"));
    }

    @Test
    void create_shouldThrowError_whenNameIsMissing() throws Exception {
        mvc.perform(
                        post("/api/products").with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"price":19.90,"inStock":true,"categoryId":10}
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_shouldReturn404_whenMissing() throws Exception {
        when(service.find(99L)).thenThrow(new EntityNotFoundException("not found"));
        mvc.perform(get("/products/99").with(jwt()))
                .andExpect(status().isNotFound());
    }



}