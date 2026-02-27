package com.demo.auth.controller;

import com.demo.auth.dto.UserAddressRequest;
import com.demo.auth.dto.UserAddressResponse;
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
import java.util.UUID;

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
    public ResponseEntity<UserAddressResponse> createAddress(@RequestBody @Valid UserAddressRequest request,
            Principal principal) {
        UserAddressResponse response = userAddressService.createAddress(principal.getName(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Update address", description = "Updates an existing address owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Address updated")
    @ApiResponse(responseCode = "404", description = "Address not found")
    @PutMapping("/{id}")
    public ResponseEntity<UserAddressResponse> update(@PathVariable UUID id,
            @RequestBody @Valid UserAddressRequest request,
            Principal principal) {
        UserAddressResponse response = userAddressService.update(id, principal.getName(), request);

        return ResponseEntity.ok().body(response);
    }

    @Operation(summary = "Delete address", description = "Deletes an address owned by the authenticated user")
    @ApiResponse(responseCode = "204", description = "Address deleted")
    @ApiResponse(responseCode = "404", description = "Address not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Principal principal) {
        userAddressService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Set default address", description = "Sets an address as the default for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Default address updated")
    @ApiResponse(responseCode = "404", description = "Address not found")
    @PatchMapping("/{id}/default")
    public ResponseEntity<UserAddressResponse> setDefault(@PathVariable UUID id, Principal principal) {
        UserAddressResponse userAddressResponse = userAddressService.setDefault(id, principal.getName());
        return ResponseEntity.ok().body(userAddressResponse);
    }
}
