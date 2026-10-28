package com.gympabon.model;

import java.time.LocalDateTime;

/**
 * Account that the gym staff uses to sign in. Its role decides which
 * functions of the system it can use.
 */
public class User extends Person {

    private String username;
    private String passwordHash;
    private Role role;
    private boolean active = true;
    private LocalDateTime lastLogin;

    public User() {
    }

    public User(String documentNumber, String firstName, String lastName, String phone, String email,
                String username, String passwordHash, Role role) {
        super(documentNumber, firstName, lastName, phone, email);
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    /**
     * Tells whether the user has the administrator role.
     *
     * @return true if the role is ADMIN
     */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /**
     * Saves the date and time of the current sign-in.
     *
     * @param dateTime moment of the sign-in
     */
    public void registerLogin(LocalDateTime dateTime) {
        this.lastLogin = dateTime;
    }

    /** Enables the account so it can sign in again. */
    public void activate() {
        this.active = true;
    }

    /** Disables the account without deleting it. */
    public void deactivate() {
        this.active = false;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
}
