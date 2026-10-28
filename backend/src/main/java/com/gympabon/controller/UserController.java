package com.gympabon.controller;

import com.gympabon.controller.dto.UserRequest;
import com.gympabon.controller.dto.UserResponse;
import com.gympabon.model.User;
import com.gympabon.service.UserService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff account endpoints. Only the administrator can use them.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final SessionManager sessionManager;

    public UserController(UserService userService, SessionManager sessionManager) {
        this.userService = userService;
        this.sessionManager = sessionManager;
    }

    /** GET /api/users */
    @GetMapping
    public List<UserResponse> list(HttpSession session) {
        sessionManager.requireAdmin(session);
        return userService.listUsers().stream().map(UserResponse::from).toList();
    }

    /** POST /api/users: creates an account with a temporary password. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@RequestBody UserRequest request, HttpSession session) {
        sessionManager.requireAdmin(session);
        return UserResponse.from(userService.createUser(request.toUser(), request.temporaryPassword()));
    }

    /** PUT /api/users/{id}: changes names, contact data or role. */
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable("id") int id, @RequestBody UserRequest request, HttpSession session) {
        sessionManager.requireAdmin(session);
        User user = request.toUser();
        user.setId(id);
        userService.updateUser(user);
        return UserResponse.from(userService.findById(id));
    }

    /** PATCH /api/users/{id}/deactivate */
    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable("id") int id, HttpSession session) {
        User admin = sessionManager.requireAdmin(session);
        userService.deactivateUser(id, admin);
    }

    /** PATCH /api/users/{id}/activate */
    @PatchMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable("id") int id, HttpSession session) {
        sessionManager.requireAdmin(session);
        userService.activateUser(id);
    }
}
