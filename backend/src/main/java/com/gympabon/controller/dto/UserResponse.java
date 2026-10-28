package com.gympabon.controller.dto;

import com.gympabon.model.User;
import java.time.LocalDateTime;

/**
 * A staff account as the frontend sees it. The password hash is never sent.
 */
public record UserResponse(int id, String documentNumber, String firstName, String lastName, String fullName,
                           String phone, String email, String username, String role, boolean active,
                           LocalDateTime lastLogin) {

    /**
     * Builds the response from a domain user.
     *
     * @param user domain user
     * @return the response without the password hash
     */
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getDocumentNumber(), user.getFirstName(), user.getLastName(),
                user.getFullName(), user.getPhone(), user.getEmail(), user.getUsername(), user.getRole().name(),
                user.isActive(), user.getLastLogin());
    }
}
