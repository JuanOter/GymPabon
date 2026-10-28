package com.gympabon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gympabon.exception.BusinessRuleException;
import com.gympabon.fake.InMemoryUserDAO;
import com.gympabon.fake.TestData;
import com.gympabon.model.Role;
import com.gympabon.model.User;
import com.gympabon.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests of {@link UserService} (RF02, RNF02). */
class UserServiceTest {

    private final PasswordHasher passwordHasher = new PasswordHasher();
    private UserService userService;
    private User admin;

    @BeforeEach
    void setUp() {
        userService = new UserService(new InMemoryUserDAO(), passwordHasher);
        admin = userService.createUser(TestData.user("admin", Role.ADMIN), "Admin12345");
    }

    @Test
    @DisplayName("Guarda la contraseña cifrada y el usuario en minúsculas")
    void createsUserWithHashedPassword() {
        User user = userService.createUser(TestData.user("  Recepcion.Tarde ", Role.RECEPTIONIST), "Clave2026*");

        assertEquals("recepcion.tarde", user.getUsername());
        assertNotEquals("Clave2026*", user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("pbkdf2$"));
        assertTrue(passwordHasher.verify("Clave2026*", user.getPasswordHash()));
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("Valida el nombre de usuario, que no se repita, la contraseña y el rol")
    void validatesNewUser() {
        assertThrows(BusinessRuleException.class,
                () -> userService.createUser(TestData.user("ab", Role.RECEPTIONIST), "Clave2026*"));
        assertThrows(BusinessRuleException.class,
                () -> userService.createUser(TestData.user("con espacio", Role.RECEPTIONIST), "Clave2026*"));
        assertThrows(BusinessRuleException.class,
                () -> userService.createUser(TestData.user("ADMIN", Role.RECEPTIONIST), "Clave2026*"));
        assertThrows(BusinessRuleException.class,
                () -> userService.createUser(TestData.user("recepcion", Role.RECEPTIONIST), "corta"));
        assertThrows(BusinessRuleException.class,
                () -> userService.createUser(TestData.user("recepcion", null), "Clave2026*"));
        assertEquals(1, userService.listUsers().size());
    }

    @Test
    @DisplayName("El administrador no puede desactivar su propia cuenta")
    void adminCannotDeactivateHimself() {
        assertThrows(BusinessRuleException.class, () -> userService.deactivateUser(admin.getId(), admin));
        assertTrue(userService.findById(admin.getId()).isActive());
    }

    @Test
    @DisplayName("Desactiva y vuelve a activar una cuenta")
    void deactivatesAndActivatesUser() {
        User user = userService.createUser(TestData.user("recepcion", Role.RECEPTIONIST), "Clave2026*");

        userService.deactivateUser(user.getId(), admin);
        assertFalse(userService.findById(user.getId()).isActive());

        userService.activateUser(user.getId());
        assertTrue(userService.findById(user.getId()).isActive());
    }

    @Test
    @DisplayName("Editar cambia datos y rol, pero no el usuario ni la contraseña")
    void updateKeepsCredentials() {
        User user = userService.createUser(TestData.user("recepcion", Role.RECEPTIONIST), "Clave2026*");
        String hash = user.getPasswordHash();
        User changes = TestData.user("otro.nombre", Role.ADMIN);
        changes.setId(user.getId());
        changes.setPhone("3207778899");

        userService.updateUser(changes);

        User stored = userService.findById(user.getId());
        assertEquals("recepcion", stored.getUsername());
        assertEquals(hash, stored.getPasswordHash());
        assertEquals(Role.ADMIN, stored.getRole());
        assertEquals("3207778899", stored.getPhone());
    }
}
