package com.demo.auth;

import com.demo.auth.dto.UserRequest;
import com.demo.auth.entity.UserRole;
import com.demo.auth.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String USERNAME = "username";
    private static final String PASSWORD = "Test@1234";

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRegisterAndLogin() throws Exception {
        // 1. Register a new user (Admin role required for POST /api/user)
        UserRequest request = new UserRequest(USERNAME, PASSWORD, Set.of(UserRole.USER, UserRole.ADMIN));

        mockMvc.perform(post("/api/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        mockMvc.perform(post("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateUserRoles() throws Exception {
        // Register a new user
        String updateUsername = "updateUser";
        UserRequest request = new UserRequest(updateUsername, PASSWORD, Set.of(UserRole.USER));
        mockMvc.perform(post("/api/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Update user with new roles
        UserRequest updateRequest = new UserRequest(updateUsername, PASSWORD, Set.of(UserRole.USER, UserRole.ADMIN));
        mockMvc.perform(put("/api/user/" + updateUsername)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // Verify roles were updated
        mockMvc.perform(get("/api/user/" + updateUsername + "/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value("ADMIN"));
    }
}
