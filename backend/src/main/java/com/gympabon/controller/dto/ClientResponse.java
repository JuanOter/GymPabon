package com.gympabon.controller.dto;

import com.gympabon.model.Client;
import java.time.LocalDate;

/**
 * A client as the frontend sees it.
 */
public record ClientResponse(int id, String documentNumber, String firstName, String lastName, String fullName,
                             String phone, String email, LocalDate birthDate, LocalDate registrationDate,
                             boolean active) {

    /**
     * Builds the response from a domain client.
     *
     * @param client domain client
     * @return the response
     */
    public static ClientResponse from(Client client) {
        return new ClientResponse(client.getId(), client.getDocumentNumber(), client.getFirstName(),
                client.getLastName(), client.getFullName(), client.getPhone(), client.getEmail(),
                client.getBirthDate(), client.getRegistrationDate(), client.isActive());
    }
}
