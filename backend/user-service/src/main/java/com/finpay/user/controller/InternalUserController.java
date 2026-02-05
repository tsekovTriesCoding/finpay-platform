package com.finpay.user.controller;

import com.finpay.user.dto.CreateUserRequest;
import com.finpay.user.dto.InternalUserResponse;
import com.finpay.user.entity.User;
import com.finpay.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal API for auth-service to manage users.
 * These endpoints should only be accessible within the internal network.
 */
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
@Slf4j
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/email/{email}")
    public ResponseEntity<InternalUserResponse> getUserByEmail(@PathVariable String email) {
        log.debug("Internal: Fetching user by email: {}", email);
        User user = userService.getUserEntityByEmail(email);
        return ResponseEntity.ok(InternalUserResponse.fromEntity(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InternalUserResponse> getUserById(@PathVariable UUID id) {
        log.debug("Internal: Fetching user by id: {}", id);
        User user = userService.getUserEntityById(id);
        return ResponseEntity.ok(InternalUserResponse.fromEntity(user));
    }

    @PostMapping
    public ResponseEntity<InternalUserResponse> createUser(@RequestBody CreateUserRequest request) {
        User user = userService.createUserFromAuth(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(InternalUserResponse.fromEntity(user));
    }

    @PatchMapping("/{id}/last-login")
    public ResponseEntity<Void> updateLastLogin(@PathVariable UUID id) {
        userService.updateLastLogin(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/exists/email/{email}")
    public ResponseEntity<Boolean> existsByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.existsByEmail(email));
    }

    @GetMapping("/exists/phone/{phoneNumber}")
    public ResponseEntity<Boolean> existsByPhoneNumber(@PathVariable String phoneNumber) {
        return ResponseEntity.ok(userService.existsByPhoneNumber(phoneNumber));
    }

    @PatchMapping("/{id}/oauth-link")
    public ResponseEntity<InternalUserResponse> linkOAuthProvider(
            @PathVariable UUID id,
            @RequestParam String provider,
            @RequestParam String providerId,
            @RequestParam(required = false) String profileImageUrl
    ) {
        User user = userService.linkOAuthProvider(id, provider, providerId, profileImageUrl);
        return ResponseEntity.ok(InternalUserResponse.fromEntity(user));
    }
}
