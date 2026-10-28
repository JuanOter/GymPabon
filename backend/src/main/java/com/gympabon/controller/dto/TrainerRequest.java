package com.gympabon.controller.dto;

import com.gympabon.model.Trainer;

/**
 * Trainer data sent by the trainer form, to register or to update.
 */
public record TrainerRequest(String documentNumber, String firstName, String lastName, String phone, String email,
                             String specialty, String workSchedule) {

    /**
     * Builds a domain trainer with the form data.
     *
     * @return a new trainer without identifier
     */
    public Trainer toTrainer() {
        return new Trainer(documentNumber, firstName, lastName, phone, email, specialty, workSchedule);
    }
}
