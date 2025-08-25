package com.demo.product.controller;

import com.demo.product.dto.CategoryDTO;
import com.demo.product.mapper.CategoryMapper;
import com.demo.product.model.Category;
import com.demo.product.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CategoryService service;

    @Test
    void create_shouldReturnCreated() throws Exception {
        var dto = new CategoryDTO(null, "Electronics");
        when(service.create(dto)).thenReturn(CategoryMapper.toDto(new Category(1L, "Electronics")));

        mvc.perform(
                        post("/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"));
    }

    @Test
    void create_shouldThrowError_whenNameIsEmpty() throws Exception {
        mvc.perform(
                        post("/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name": ""}
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_shouldReturn404_whenMissing() throws Exception {
        when(service.find(99L)).thenThrow(new EntityNotFoundException("not found"));
        mvc.perform(get("/products/99"))
                .andExpect(status().isNotFound());
    }
}