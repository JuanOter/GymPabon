package com.gympabon.fake;

import com.gympabon.dao.ClientDAO;
import com.gympabon.model.Client;
import java.util.List;

/**
 * In-memory {@link ClientDAO} for unit tests. Like the JDBC version, every
 * read returns a new {@link Client} object without its memberships loaded.
 */
public class InMemoryClientDAO extends InMemoryDAO<Client> implements ClientDAO {

    @Override
    protected int idOf(Client client) {
        return client.getId();
    }

    @Override
    protected void assignId(Client client, int id) {
        client.setId(id);
    }

    @Override
    protected Client copy(Client client) {
        Client copy = new Client(client.getDocumentNumber(), client.getFirstName(), client.getLastName(),
                client.getPhone(), client.getEmail(), client.getBirthDate(), client.getRegistrationDate());
        copy.setId(client.getId());
        copy.setActive(client.isActive());
        return copy;
    }

    @Override
    public Client findByDocument(String documentNumber) {
        return rows.values().stream().filter(c -> c.getDocumentNumber().equals(documentNumber))
                .findFirst().map(this::copy).orElse(null);
    }

    @Override
    public List<Client> search(String text) {
        String lower = text.trim().toLowerCase();
        return rows.values().stream()
                .filter(c -> c.getDocumentNumber().contains(lower) || c.getFullName().toLowerCase().contains(lower))
                .map(this::copy)
                .toList();
    }
}
