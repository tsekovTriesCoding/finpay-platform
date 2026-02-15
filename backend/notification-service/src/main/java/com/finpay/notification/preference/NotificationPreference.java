package com.finpay.notification.preference;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private boolean emailEnabled;

    @Column(nullable = false)
    private boolean smsEnabled;

    @Column(nullable = false)
    private boolean pushEnabled;

    @Column(nullable = false)
    private boolean inAppEnabled;

    // Notification type preferences
    @Column(nullable = false)
    private boolean paymentNotifications;

    @Column(nullable = false)
    private boolean securityNotifications;

    @Column(nullable = false)
    private boolean promotionalNotifications;

    @Column(nullable = false)
    private boolean systemNotifications;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
