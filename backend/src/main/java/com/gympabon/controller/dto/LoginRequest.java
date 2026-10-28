package com.gympabon.controller.dto;

/**
 * Credentials typed on the sign-in screen.
 *
 * @param username username
 * @param password password in plain text, only used to check it
 */
public record LoginRequest(String username, String password) {
}
