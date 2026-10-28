package com.gympabon.controller.dto;

import com.gympabon.model.Role;
import com.gympabon.model.User;

/**
 * Account data sent by the user form. The temporary password is only used
 * when the account is created.
 */
public record UserRequest(String documentNumber, String firstName, String lastName, String phone, String email,
                          String username, Role role, String temporaryPassword) {

    /**
     * Builds a domain user with the form data, without password.
     *
     * @return a new user without identifier
     */
    public User toUser() {
        return new User(documentNumber, firstName, lastName, phone, email, username, null, role);
    }
}
