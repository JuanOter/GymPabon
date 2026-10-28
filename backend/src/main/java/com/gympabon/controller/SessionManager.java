package com.gympabon.controller;

import com.gympabon.dao.UserDAO;
import com.gympabon.exception.NotAuthenticatedException;
import com.gympabon.model.User;
import com.gympabon.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * Keeps track of the signed-in user in the HTTP session. Every controller asks
 * this class who is making the request, so the permission checks live in one
 * place.
 */
@Component
public class SessionManager {

    private static final String USER_ID = "userId";

    private final UserDAO userDAO;
    private final AuthService authService;

    public SessionManager(UserDAO userDAO, AuthService authService) {
        this.userDAO = userDAO;
        this.authService = authService;
    }

    /**
     * Remembers the user in the session after a successful sign-in.
     *
     * @param session HTTP session of the browser
     * @param user    signed-in user
     */
    public void signIn(HttpSession session, User user) {
        session.setAttribute(USER_ID, user.getId());
    }

    /**
     * Ends the session.
     *
     * @param session HTTP session of the browser
     */
    public void signOut(HttpSession session) {
        session.invalidate();
    }

    /**
     * Returns the signed-in user, loaded again so a disabled account loses
     * access at once.
     *
     * @param session HTTP session of the browser
     * @return the signed-in user
     * @throws NotAuthenticatedException if nobody signed in or the account is disabled
     */
    public User requireUser(HttpSession session) {
        Object userId = session.getAttribute(USER_ID);
        if (!(userId instanceof Integer id)) {
            throw new NotAuthenticatedException("Debe iniciar sesión");
        }
        User user = userDAO.findById(id);
        if (user == null || !user.isActive()) {
            session.invalidate();
            throw new NotAuthenticatedException("La sesión ya no es válida; inicie sesión de nuevo");
        }
        return user;
    }

    /**
     * Returns the signed-in user if it is an administrator.
     *
     * @param session HTTP session of the browser
     * @return the signed-in administrator
     * @throws NotAuthenticatedException if nobody signed in
     * @throws com.gympabon.exception.BusinessRuleException if the user is not an administrator
     */
    public User requireAdmin(HttpSession session) {
        User user = requireUser(session);
        authService.requireAdmin(user);
        return user;
    }
}
