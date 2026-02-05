package com.finpay.auth.client;

import com.finpay.auth.dto.CreateUserRequest;
import com.finpay.auth.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Feign client for communicating with user-service.
 */
@FeignClient(name = "user-service", path = "/api/v1/internal/users")
public interface UserServiceClient {

    @GetMapping("/email/{email}")
    UserDto getUserByEmail(@PathVariable("email") String email);

    @GetMapping("/{id}")
    UserDto getUserById(@PathVariable("id") UUID id);

    @PostMapping
    UserDto createUser(@RequestBody CreateUserRequest request);

    @PatchMapping("/{id}/last-login")
    void updateLastLogin(@PathVariable("id") UUID id);

    @GetMapping("/exists/email/{email}")
    boolean existsByEmail(@PathVariable("email") String email);

    @GetMapping("/exists/phone/{phoneNumber}")
    boolean existsByPhoneNumber(@PathVariable("phoneNumber") String phoneNumber);

    @PatchMapping("/{id}/oauth-link")
    UserDto linkOAuthProvider(
            @PathVariable("id") UUID id,
            @RequestParam("provider") String provider,
            @RequestParam("providerId") String providerId,
            @RequestParam(value = "profileImageUrl", required = false) String profileImageUrl
    );
}
