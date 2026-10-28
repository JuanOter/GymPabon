package com.gympabon.service;

import com.gympabon.dao.UserDAO;
import com.gympabon.exception.BusinessRuleException;
import com.gympabon.model.User;
import com.gympabon.util.PasswordHasher;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * Signs the staff in and checks their permissions.
 */
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Usuario o contraseña incorrectos";

    private final UserDAO userDAO;
    private final PasswordHasher passwordHasher;

    public AuthService(UserDAO userDAO, PasswordHasher passwordHasher) {
        this.userDAO = userDAO;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Checks the username and password and saves the moment of the sign-in.
     *
     * @param username username typed by the user
     * @param password password typed by the user
     * @return the signed-in user
     * @throws BusinessRuleException if the credentials are wrong or the account is disabled
     */
    public User login(String username, String password) {
        if (username == null || password == null) {
            throw new BusinessRuleException(INVALID_CREDENTIALS);
        }
        User user = userDAO.findByUsername(username.trim().toLowerCase());
        if (user == null || !passwordHasher.verify(password, user.getPasswordHash())) {
            throw new BusinessRuleException(INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new BusinessRuleException("La cuenta está desactivada; comuníquese con el administrador");
        }
        user.registerLogin(LocalDateTime.now());
        userDAO.update(user);
        return user;
    }

    /**
     * Stops the operation if the user is not an administrator. It is called
     * before the functions that only the administrator can use.
     *
     * @param user signed-in user
     * @throws BusinessRuleException if the user is not an administrator
     */
    public void requireAdmin(User user) {
        if (user == null || !user.isAdmin()) {
            throw new BusinessRuleException("Esta función es solo para el administrador");
        }
    }
}
