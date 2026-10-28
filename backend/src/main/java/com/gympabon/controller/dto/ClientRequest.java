package com.gympabon.controller.dto;

import com.gympabon.model.Client;
import java.time.LocalDate;

/**
 * Client data sent by the client form, to register or to update.
 */
public record ClientRequest(String documentNumber, String firstName, String lastName, String phone, String email,
                            LocalDate birthDate) {

    /**
     * Builds a domain client with the form data.
     *
     * @return a new client without identifier
     */
    public Client toClient() {
        return new Client(documentNumber, firstName, lastName, phone, email, birthDate, null);
    }
}
