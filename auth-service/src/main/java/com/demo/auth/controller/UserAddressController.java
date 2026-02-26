package com.demo.auth.controller;

import com.demo.auth.dto.UserAddressRequest;
import com.demo.auth.dto.UserAddressResponse;
import com.demo.auth.entity.UserAddress;
import com.demo.auth.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Addresses", description = "Manage saved shipping addresses for the authenticated users")
public class UserAddressController {

    private final UserAddressService userAddressService;

    public UserAddressController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    @Operation(summary = "List addresses", description = "Returns all saved addresses for the authenticated user")
    @ApiResponse(responseCode = "200", description = "List returned")
    @GetMapping
    public ResponseEntity<List<UserAddressResponse>> getAll(Principal principal) {
        return ResponseEntity.ok(userAddressService.getAddresses(principal.getName()));
    }

    @Operation(summary = "Create user address", description = "Creates a new address for the authenticated user")
    @ApiResponse(responseCode = "201", description = "Address created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PostMapping
    public ResponseEntity<Void> createAddress(@RequestBody @Valid UserAddressRequest request, Principal principal) {
        UserAddress savedUserAddress = userAddressService.createAddress(principal.getName(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(savedUserAddress.getId())
                .toUri();
        return ResponseEntity.created(location).build();
    }
}
