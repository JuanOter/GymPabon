package com.gympabon.dao;

import com.gympabon.model.Client;
import java.util.List;

/**
 * Data access for gym clients.
 */
public interface ClientDAO extends GenericDAO<Client, Integer> {

    /**
     * Finds a client by document number.
     *
     * @param documentNumber document to look for
     * @return the client, or null if there is no client with that document
     */
    Client findByDocument(String documentNumber);

    /**
     * Finds the clients whose document or full name contains the text.
     *
     * @param text part of the document or of the name
     * @return the matching clients ordered by name
     */
    List<Client> search(String text);
}
