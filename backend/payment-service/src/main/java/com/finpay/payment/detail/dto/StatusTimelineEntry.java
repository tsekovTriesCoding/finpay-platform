package com.finpay.payment.detail.dto;

import java.time.LocalDateTime;

/**
 * A single entry in a transaction status timeline.
 * Represents a step the transaction has passed through (or is expected to pass through).
 */
public record StatusTimelineEntry(
        String status,
        String label,
        String description,
        LocalDateTime timestamp,
        boolean completed,
        boolean current,
        boolean failed
) {
    public static StatusTimelineEntry completed(String status, String label, String description, LocalDateTime timestamp) {
        return new StatusTimelineEntry(status, label, description, timestamp, true, false, false);
    }

    public static StatusTimelineEntry current(String status, String label, String description, LocalDateTime timestamp) {
        return new StatusTimelineEntry(status, label, description, timestamp, false, true, false);
    }

    public static StatusTimelineEntry pending(String status, String label, String description) {
        return new StatusTimelineEntry(status, label, description, null, false, false, false);
    }

    public static StatusTimelineEntry failed(String status, String label, String description, LocalDateTime timestamp) {
        return new StatusTimelineEntry(status, label, description, timestamp, false, false, true);
    }
}
