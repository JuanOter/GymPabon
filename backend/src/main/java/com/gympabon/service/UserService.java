package com.gympabon.service;

import com.gympabon.dao.UserDAO;
import com.gympabon.exception.BusinessRuleException;
import com.gympabon.exception.NotFoundException;
import com.gympabon.model.User;
import com.gympabon.util.PasswordHasher;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Business rules for the staff accounts: creation, changes and activation.
 * Only the administrator uses these functions.
 */
@Service
public class UserService {

    private static final Pattern USERNAME = Pattern.compile("[a-zA-Z0-9._]{4,30}");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserDAO userDAO;
    private final PasswordHasher passwordHasher;

    public UserService(UserDAO userDAO, PasswordHasher passwordHasher) {
        this.userDAO = userDAO;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Creates an active account with an encrypted temporary password.
     *
     * @param user              new user with its data and role
     * @param temporaryPassword password the user will use the first time
     * @return the saved user with its identifier
     * @throws BusinessRuleException if the data is invalid, the username is taken
     *                               or the password is too short
     */
    public User createUser(User user, String temporaryPassword) {
        PersonValidator.validate(user);
        validateRole(user);
        String username = user.getUsername() == null ? "" : user.getUsername().trim().toLowerCase();
        if (!USERNAME.matcher(username).matches()) {
            throw new BusinessRuleException(
                    "El usuario debe tener entre 4 y 30 caracteres, sin espacios: letras, números, punto o guion bajo");
        }
        if (userDAO.findByUsername(username) != null) {
            throw new BusinessRuleException("El usuario " + username + " ya existe");
        }
        validatePassword(temporaryPassword);
        user.setUsername(username);
        user.setPasswordHash(passwordHasher.hash(temporaryPassword));
        user.setActive(true);
        user.setLastLogin(null);
        userDAO.save(user);
        return user;
    }

    /**
     * Saves the changes of name, contact data or role. The username, the
     * password and the status are kept.
     *
     * @param user user with its identifier and the new data
     * @throws NotFoundException     if the user does not exist
     * @throws BusinessRuleException if the data is invalid
     */
    public void updateUser(User user) {
        User stored = findById(user.getId());
        PersonValidator.validate(user);
        validateRole(user);
        stored.setDocumentNumber(user.getDocumentNumber());
        stored.setFirstName(user.getFirstName());
        stored.setLastName(user.getLastName());
        stored.setPhone(user.getPhone());
        stored.setEmail(user.getEmail());
        stored.setRole(user.getRole());
        userDAO.update(stored);
    }

    /**
     * Disables an account. The administrator cannot disable their own account.
     *
     * @param userId identifier of the account to disable
     * @param admin  administrator who makes the change
     * @throws BusinessRuleException if the administrator tries to disable their own account
     */
    public void deactivateUser(int userId, User admin) {
        if (admin != null && admin.getId() == userId) {
            throw new BusinessRuleException("No puede desactivar su propia cuenta");
        }
        User user = findById(userId);
        user.deactivate();
        userDAO.update(user);
    }

    /**
     * Enables an account that was disabled.
     *
     * @param userId identifier of the account
     */
    public void activateUser(int userId) {
        User user = findById(userId);
        user.activate();
        userDAO.update(user);
    }

    /**
     * Returns every account with its role and status.
     *
     * @return all the users
     */
    public List<User> listUsers() {
        return userDAO.findAll();
    }

    /**
     * Finds a user by its identifier.
     *
     * @param userId identifier of the user
     * @return the user
     * @throws NotFoundException if the user does not exist
     */
    public User findById(int userId) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new NotFoundException("El usuario no existe");
        }
        return user;
    }

    private void validateRole(User user) {
        if (user.getRole() == null) {
            throw new BusinessRuleException("El rol es obligatorio");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessRuleException("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
    }
}
