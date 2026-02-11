package com.demo.auth.controller;

import com.demo.auth.dto.UserRequest;
import com.demo.auth.dto.UserPatchRequest;
import com.demo.auth.dto.UserResponse;
import com.demo.auth.entity.User;
import com.demo.auth.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/api/user")
@Validated
@Tag(name = "User Management", description = "Endpoints for managing users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get user by username", description = "Retrieves public user information")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable @NotBlank String username) {
        User user = userService.findByUsername(username);
        UserResponse response = new UserResponse(
                user.getUsername(),
                user.getRolesAsString(),
                LocalDateTime.ofInstant(user.getCreatedAt(), ZoneOffset.UTC),
                LocalDateTime.ofInstant(user.getUpdatedAt(), ZoneOffset.UTC),
                user.getCreatedBy(),
                user.getUpdatedBy());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user roles", description = "Retrieves user roles by username")
    @ApiResponse(responseCode = "200", description = "Roles found")
    @GetMapping("/{username}/roles")
    public ResponseEntity<Set<String>> getUserRoles(@PathVariable @NotBlank String username) {
        SortedSet<String> roles = userService.rolesByUsername(username);
        return ResponseEntity.ok(roles);
    }

    @Operation(summary = "Get user scopes", description = "Retrieves user scopes by username")
    @ApiResponse(responseCode = "200", description = "Scopes found")
    @GetMapping("/{username}/scopes")
    public ResponseEntity<List<String>> getUserScopes(@PathVariable @NotBlank String username) {
        List<String> scopes = userService.scopesByUsername(username);
        return ResponseEntity.ok(scopes);
    }

    @Operation(summary = "Create user", description = "Creates a new user (Admin only)")
    @ApiResponse(responseCode = "201", description = "User created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> createUser(@RequestBody @Validated(UserRequest.Create.class) UserRequest request) {
        User savedUser = userService.createUser(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{username}")
                .buildAndExpand(savedUser.getUsername())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "Update user", description = "Updates an existing user (Admin only)")
    @ApiResponse(responseCode = "200", description = "User updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PutMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateUser(@PathVariable @NotBlank String username,
                                           @RequestBody @Validated(UserRequest.Update.class) UserRequest request) {
        userService.updateUser(username, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete user", description = "Deletes an existing user (Admin only)")
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    @DeleteMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable @NotBlank String username) {
        userService.deleteUser(username);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Patch user status", description = "Partially update user status (Admin only)")
    @ApiResponse(responseCode = "200", description = "User status updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PatchMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> patchUser(@PathVariable @NotBlank String username,
                                          @RequestBody @Valid UserPatchRequest request) {
        userService.patchUser(username, request);
        return ResponseEntity.ok().build();
    }
}
