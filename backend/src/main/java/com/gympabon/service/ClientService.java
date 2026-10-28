package com.gympabon.service;

import com.gympabon.dao.ClientDAO;
import com.gympabon.exception.BusinessRuleException;
import com.gympabon.exception.NotFoundException;
import com.gympabon.model.Client;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Business rules for registering, searching and deactivating gym clients.
 */
@Service
public class ClientService {

    private final ClientDAO clientDAO;

    public ClientService(ClientDAO clientDAO) {
        this.clientDAO = clientDAO;
    }

    /**
     * Validates the client data and saves it as an active client registered today.
     *
     * @param client new client
     * @return the saved client with its identifier
     * @throws BusinessRuleException if the data is invalid or the document is already registered
     */
    public Client registerClient(Client client) {
        PersonValidator.validate(client);
        validateBirthDate(client.getBirthDate());
        ensureDocumentIsFree(client.getDocumentNumber(), 0);
        client.setRegistrationDate(LocalDate.now());
        client.setActive(true);
        clientDAO.save(client);
        return client;
    }

    /**
     * Validates and saves the changes of an existing client.
     *
     * @param client client with its identifier and the new data
     * @throws NotFoundException     if the client does not exist
     * @throws BusinessRuleException if the data is invalid or the document belongs to another client
     */
    public void updateClient(Client client) {
        Client stored = findById(client.getId());
        PersonValidator.validate(client);
        validateBirthDate(client.getBirthDate());
        ensureDocumentIsFree(client.getDocumentNumber(), client.getId());
        client.setRegistrationDate(stored.getRegistrationDate());
        client.setActive(stored.isActive());
        clientDAO.update(client);
    }

    /**
     * Finds a client by document number.
     *
     * @param documentNumber document to look for
     * @return the client
     * @throws NotFoundException if there is no client with that document
     */
    public Client findByDocument(String documentNumber) {
        Client client = clientDAO.findByDocument(documentNumber == null ? "" : documentNumber.trim());
        if (client == null) {
            throw new NotFoundException("No existe un cliente con el documento " + documentNumber);
        }
        return client;
    }

    /**
     * Finds a client by its identifier.
     *
     * @param clientId identifier of the client
     * @return the client
     * @throws NotFoundException if the client does not exist
     */
    public Client findById(int clientId) {
        Client client = clientDAO.findById(clientId);
        if (client == null) {
            throw new NotFoundException("El cliente no existe");
        }
        return client;
    }

    /**
     * Finds clients by document or name. An empty text returns every client.
     *
     * @param text part of the document or of the name
     * @return the matching clients
     */
    public List<Client> searchClients(String text) {
        if (text == null || text.isBlank()) {
            return clientDAO.findAll();
        }
        return clientDAO.search(text);
    }

    /**
     * Deactivates a client without deleting its history.
     *
     * @param clientId identifier of the client
     * @throws NotFoundException if the client does not exist
     */
    public void deactivateClient(int clientId) {
        Client client = findById(clientId);
        client.deactivate();
        clientDAO.update(client);
    }

    private void ensureDocumentIsFree(String documentNumber, int clientId) {
        Client existing = clientDAO.findByDocument(documentNumber);
        if (existing != null && existing.getId() != clientId) {
            throw new BusinessRuleException("Ya existe un cliente con el documento " + documentNumber);
        }
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate != null && birthDate.isAfter(LocalDate.now().minusYears(14))) {
            throw new BusinessRuleException("El cliente debe tener al menos 14 años");
        }
    }
}
