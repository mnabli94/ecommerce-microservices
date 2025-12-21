package com.demo.auth.controller;

import com.demo.auth.dto.CreateUserRequest;
import com.demo.auth.entity.User;
import com.demo.auth.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.function.BiFunction;
import static org.hamcrest.Matchers.*;
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

    static final BiFunction<String, Integer, String> MIN_SIZE_MSG = "minimal %s size is %d"::formatted;
    @Test
    void addUser_shouldReturnSuccess() throws Exception {
        CreateUserRequest request = new CreateUserRequest("USERNAME", "PASSWORD", "USER");
        given(userService.createUser(request)).willReturn(toUserEntity(request));
        mockMvc.perform(post("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(header().string("Location", "http://localhost/api/user/USERNAME"))
                .andExpect(status().isCreated());
    }

    @Test
    void addUser_shouldReturnNotValidPayload_whenValidationFails() throws Exception {
        CreateUserRequest request = new CreateUserRequest("A", "PASS", "");
        given(userService.createUser(request)).willReturn(toUserEntity(request));
        mockMvc.perform(post("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("error").value("NOT_VALID"))
                .andExpect(jsonPath("timestamp").exists())
                .andExpect(jsonPath("details.username", hasItem(containsStringIgnoringCase(MIN_SIZE_MSG.apply("username",2)))))
                .andExpect(jsonPath("details.password", hasItem(containsStringIgnoringCase(MIN_SIZE_MSG.apply("password", 6)))))
                .andExpect(jsonPath("details.roles", containsInAnyOrder("must not be blank")));

        verifyNoInteractions(userService);
    }

    @Test
    void addUser_shouldReturnOnlyRolesError_whenOnlyRolesBlank() throws Exception {
        CreateUserRequest req = new CreateUserRequest("AZE", "PASSWORD", ""); // password OK

        mockMvc.perform(post("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("NOT_VALID"))
                .andExpect(jsonPath("$.details.roles",containsInAnyOrder("must not be blank")))
                .andExpect(jsonPath("$.details.username").doesNotExist())
                .andExpect(jsonPath("$.details.password").doesNotExist());

        verifyNoInteractions(userService);
    }

    private static User toUserEntity(CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(request.password());
        user.setRoles(request.roles());
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