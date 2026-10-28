package com.gympabon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gympabon.exception.BusinessRuleException;
import com.gympabon.exception.NotFoundException;
import com.gympabon.fake.InMemoryClientDAO;
import com.gympabon.fake.TestData;
import com.gympabon.model.Client;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests of {@link ClientService} (RF03, RF04). */
class ClientServiceTest {

    private InMemoryClientDAO clientDAO;
    private ClientService clientService;

    @BeforeEach
    void setUp() {
        clientDAO = new InMemoryClientDAO();
        clientService = new ClientService(clientDAO);
    }

    @Test
    @DisplayName("Registra un cliente activo con la fecha de hoy y limpia los espacios")
    void registersClient() {
        Client client = TestData.client(" 1065123456 ");
        client.setFirstName("  María   José ");

        clientService.registerClient(client);

        Client stored = clientService.findById(client.getId());
        assertEquals("1065123456", stored.getDocumentNumber());
        assertEquals("María José", stored.getFirstName());
        assertEquals(LocalDate.now(), stored.getRegistrationDate());
        assertTrue(stored.isActive());
    }

    @Test
    @DisplayName("No permite dos clientes con el mismo documento")
    void rejectsDuplicateDocument() {
        clientService.registerClient(TestData.client("1065123456"));

        assertThrows(BusinessRuleException.class, () -> clientService.registerClient(TestData.client("1065123456")));
        assertEquals(1, clientDAO.count());
    }

    @Test
    @DisplayName("Valida documento, nombres, teléfono y correo")
    void validatesPersonalData() {
        assertThrows(BusinessRuleException.class, () -> clientService.registerClient(TestData.client("12AB45")));
        assertThrows(BusinessRuleException.class, () -> clientService.registerClient(TestData.client("123")));

        Client noName = TestData.client("1065123456");
        noName.setFirstName(" ");
        assertThrows(BusinessRuleException.class, () -> clientService.registerClient(noName));

        Client badPhone = TestData.client("1065123456");
        badPhone.setPhone("300-ABC");
        assertThrows(BusinessRuleException.class, () -> clientService.registerClient(badPhone));

        Client badEmail = TestData.client("1065123456");
        badEmail.setEmail("correo-sin-arroba");
        assertThrows(BusinessRuleException.class, () -> clientService.registerClient(badEmail));
        assertEquals(0, clientDAO.count());
    }

    @Test
    @DisplayName("El cliente debe tener al menos 14 años")
    void requiresMinimumAge() {
        Client young = TestData.client("1065123456");
        young.setBirthDate(LocalDate.now().minusYears(13));

        assertThrows(BusinessRuleException.class, () -> clientService.registerClient(young));
    }

    @Test
    @DisplayName("Actualizar conserva la fecha de registro y el estado")
    void updateKeepsRegistrationAndStatus() {
        Client client = clientService.registerClient(TestData.client("1065123456"));
        Client changes = TestData.client("1065123456");
        changes.setId(client.getId());
        changes.setPhone("3015550000");
        changes.setRegistrationDate(LocalDate.of(2000, 1, 1));
        changes.setActive(false);

        clientService.updateClient(changes);

        Client stored = clientService.findById(client.getId());
        assertEquals("3015550000", stored.getPhone());
        assertEquals(LocalDate.now(), stored.getRegistrationDate());
        assertTrue(stored.isActive());
    }

    @Test
    @DisplayName("No permite cambiar el documento por el de otro cliente")
    void updateRejectsDocumentOfAnotherClient() {
        clientService.registerClient(TestData.client("1065123456"));
        Client second = clientService.registerClient(TestData.client("1065654321"));
        second.setDocumentNumber("1065123456");

        assertThrows(BusinessRuleException.class, () -> clientService.updateClient(second));
    }

    @Test
    @DisplayName("Busca por documento o nombre y sin texto devuelve todos")
    void searchesClients() {
        clientService.registerClient(TestData.client("1065123456"));
        Client other = TestData.client("1065654321");
        other.setFirstName("Carolina");
        clientService.registerClient(other);

        assertEquals(2, clientService.searchClients("").size());
        assertEquals(1, clientService.searchClients("carolina").size());
        assertEquals(1, clientService.searchClients("123456").size());
        assertEquals("Carolina", clientService.findByDocument(" 1065654321 ").getFirstName());
    }

    @Test
    @DisplayName("Un documento o id inexistente lanza NotFoundException")
    void unknownClientFails() {
        assertThrows(NotFoundException.class, () -> clientService.findByDocument("999999"));
        assertThrows(NotFoundException.class, () -> clientService.findById(99));
    }

    @Test
    @DisplayName("Desactivar un cliente no lo borra y se puede reactivar")
    void deactivatesAndActivatesClient() {
        Client client = clientService.registerClient(TestData.client("1065123456"));

        clientService.deactivateClient(client.getId());
        assertFalse(clientService.findById(client.getId()).isActive());
        assertEquals(1, clientDAO.count());

        clientService.activateClient(client.getId());
        assertTrue(clientService.findById(client.getId()).isActive());
        assertThrows(NotFoundException.class, () -> clientService.activateClient(99));
    }
}
