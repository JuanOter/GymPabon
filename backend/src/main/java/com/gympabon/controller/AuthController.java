package com.gympabon.controller;

import com.gympabon.controller.dto.LoginRequest;
import com.gympabon.controller.dto.UserResponse;
import com.gympabon.model.User;
import com.gympabon.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign-in and sign-out of the gym staff.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionManager sessionManager;

    public AuthController(AuthService authService, SessionManager sessionManager) {
        this.authService = authService;
        this.sessionManager = sessionManager;
    }

    /** POST /api/auth/login: checks the credentials and opens the session. */
    @PostMapping("/login")
    public UserResponse login(@RequestBody LoginRequest request, HttpSession session) {
        User user = authService.login(request.username(), request.password());
        sessionManager.signIn(session, user);
        return UserResponse.from(user);
    }

    /** POST /api/auth/logout: closes the session. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        sessionManager.signOut(session);
    }

    /** GET /api/auth/me: returns the signed-in user, used by every page on load. */
    @GetMapping("/me")
    public UserResponse currentUser(HttpSession session) {
        return UserResponse.from(sessionManager.requireUser(session));
    }
}
