package com.gympabon.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gympabon.exception.BusinessRuleException;
import com.gympabon.fake.InMemoryUserDAO;
import com.gympabon.fake.TestData;
import com.gympabon.model.Role;
import com.gympabon.model.User;
import com.gympabon.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests of {@link AuthService} (RF01). */
class AuthServiceTest {

    private static final String PASSWORD = "Clave2026*";

    private UserService userService;
    private AuthService authService;
    private User receptionist;

    @BeforeEach
    void setUp() {
        InMemoryUserDAO userDAO = new InMemoryUserDAO();
        PasswordHasher passwordHasher = new PasswordHasher();
        userService = new UserService(userDAO, passwordHasher);
        authService = new AuthService(userDAO, passwordHasher);
        receptionist = userService.createUser(TestData.user("recepcion", Role.RECEPTIONIST), PASSWORD);
    }

    @Test
    @DisplayName("Inicia sesión sin importar mayúsculas en el usuario y guarda el último acceso")
    void logsIn() {
        User user = authService.login(" Recepcion ", PASSWORD);

        assertEquals(receptionist.getId(), user.getId());
        assertNotNull(userService.findById(user.getId()).getLastLogin());
    }

    @Test
    @DisplayName("Contraseña incorrecta o usuario inexistente dan el mismo mensaje")
    void rejectsWrongCredentials() {
        BusinessRuleException wrongPassword = assertThrows(BusinessRuleException.class,
                () -> authService.login("recepcion", "otraClave1"));
        BusinessRuleException unknownUser = assertThrows(BusinessRuleException.class,
                () -> authService.login("nadie", PASSWORD));

        assertEquals(wrongPassword.getMessage(), unknownUser.getMessage());
        assertThrows(BusinessRuleException.class, () -> authService.login(null, null));
    }

    @Test
    @DisplayName("Una cuenta desactivada no puede iniciar sesión")
    void rejectsInactiveAccount() {
        userService.deactivateUser(receptionist.getId(), null);

        assertThrows(BusinessRuleException.class, () -> authService.login("recepcion", PASSWORD));
    }

    @Test
    @DisplayName("Solo el administrador pasa la verificación de administrador")
    void requiresAdmin() {
        User admin = TestData.user("admin", Role.ADMIN);

        assertDoesNotThrow(() -> authService.requireAdmin(admin));
        assertThrows(BusinessRuleException.class, () -> authService.requireAdmin(receptionist));
        assertThrows(BusinessRuleException.class, () -> authService.requireAdmin(null));
    }
}
