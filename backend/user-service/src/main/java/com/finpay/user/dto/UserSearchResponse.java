package com.finpay.user.dto;

import com.finpay.user.entity.User;

import java.util.UUID;

/**
 * Lightweight user response for search results.
 * Only contains public information needed for selecting a transfer recipient.
 */
public record UserSearchResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String profileImageUrl
) {
    public static UserSearchResponse fromEntity(User user) {
        return new UserSearchResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getProfileImageUrl()
        );
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
