package com.finpay.notification.preference;

import com.finpay.notification.preference.dto.NotificationPreferenceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/user/{userId}/preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<NotificationPreference> getPreferences(@PathVariable UUID userId) {
        NotificationPreference preferences = preferenceService.getOrCreatePreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    @PutMapping
    public ResponseEntity<NotificationPreference> updatePreferences(
            @PathVariable UUID userId,
            @RequestBody NotificationPreferenceRequest request) {
        NotificationPreference preferences = preferenceService.updatePreferences(userId, request);
        return ResponseEntity.ok(preferences);
    }
}
