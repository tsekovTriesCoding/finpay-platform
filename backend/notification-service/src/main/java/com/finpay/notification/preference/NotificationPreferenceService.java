package com.finpay.notification.preference;

import com.finpay.notification.notification.NotificationMapper;
import com.finpay.notification.preference.dto.NotificationPreferenceRequest;
import com.finpay.notification.preference.event.NotificationPreferenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationPreferenceEventProducer preferenceEventProducer;
    private final NotificationMapper notificationMapper;

    public NotificationPreference getOrCreatePreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
    }

    private NotificationPreference createDefaultPreferences(UUID userId) {
        NotificationPreference preference = NotificationPreference.builder()
                .userId(userId)
                .emailEnabled(true)
                .smsEnabled(false)
                .pushEnabled(true)
                .inAppEnabled(true)
                .paymentNotifications(true)
                .securityNotifications(true)
                .promotionalNotifications(false)
                .systemNotifications(true)
                .build();
        return preferenceRepository.save(preference);
    }

    public NotificationPreference updatePreferences(UUID userId, NotificationPreferenceRequest request) {
        NotificationPreference preference = getOrCreatePreferences(userId);
        notificationMapper.updatePreferences(request, preference);
        NotificationPreference saved = preferenceRepository.save(preference);

        // Publish preference change event to Kafka
        publishPreferenceEvent(saved, NotificationPreferenceEvent.EventType.PREFERENCES_UPDATED);

        return saved;
    }

    private void publishPreferenceEvent(NotificationPreference pref, NotificationPreferenceEvent.EventType eventType) {
        NotificationPreferenceEvent event = new NotificationPreferenceEvent(
                pref.getUserId(),
                pref.isEmailEnabled(),
                pref.isSmsEnabled(),
                pref.isPushEnabled(),
                pref.isInAppEnabled(),
                pref.isPaymentNotifications(),
                pref.isSecurityNotifications(),
                pref.isPromotionalNotifications(),
                pref.isSystemNotifications(),
                eventType.name(),
                LocalDateTime.now()
        );
        preferenceEventProducer.sendPreferenceEvent(event);
    }
}
