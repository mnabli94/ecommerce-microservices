package com.demo.auth.controller;

import com.demo.auth.dto.CreateUserRequest;
import com.demo.auth.dto.UserResponse;
import com.demo.auth.entity.User;
import com.demo.auth.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable @NotBlank String username) {
        User user = userService.findByUsername(username);
        UserResponse response = new UserResponse(user.getUsername(), user.getRoles());
        return ResponseEntity.ok(response);
    }

    @PostMapping()
    public ResponseEntity<Void> createUser(@RequestBody @Valid CreateUserRequest request) {
        User savedUser = userService.createUser(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{username}")
                .buildAndExpand(savedUser.getUsername())
                .toUri();
        return ResponseEntity.created(location).build();
    }
}
