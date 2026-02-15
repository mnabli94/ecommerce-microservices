package com.demo.auth.controller;

import com.demo.auth.dto.UserRequest;
import com.demo.auth.entity.User;
import com.demo.auth.entity.UserRole;
import com.demo.auth.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;
import java.util.function.BiFunction;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    UserService userService;

    @MockBean
    StringRedisTemplate redisTemplate;

    static final BiFunction<String, Integer, String> MIN_SIZE_MSG = "minimal %s size is %d"::formatted;

    @Test
    void addUser_shouldReturnSuccess() throws Exception {
        UserRequest request = new UserRequest("USERNAME", "PASSWORD", Set.of(UserRole.USER));
        given(userService.createUser(request)).willReturn(toUserEntity(request));
        mockMvc.perform(post("/api/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(header().string("Location", "http://localhost/api/user/USERNAME"))
                .andExpect(status().isCreated());
    }

    @Test
    void addUser_shouldReturnNotValidPayload_whenValidationFails() throws Exception {
        UserRequest request = new UserRequest("A", "PASS", Set.of()); // Username trop court, password trop court, pas
                                                                      // de rôles
        // Validation fails before reaching service, so no mock needed
        mockMvc.perform(post("/api/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("error").value("NOT_VALID"))
                .andExpect(jsonPath("timestamp").exists());
    }

    @Test
    void addUser_shouldReturnValidationError_whenRolesEmpty() throws Exception {
        UserRequest req = new UserRequest("AZE", "PASSWORD", Set.of()); // password OK, but no roles

        mockMvc.perform(post("/api/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("NOT_VALID"));

        verifyNoInteractions(userService);
    }

    private static User toUserEntity(UserRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(request.password());
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        request.roles().forEach(user::addRole);
        return user;
    }

    private static String asJsonString(final Object obj) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final String jsonContent = mapper.writeValueAsString(obj);
            return jsonContent;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}