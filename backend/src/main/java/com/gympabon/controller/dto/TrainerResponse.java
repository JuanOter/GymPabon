package com.gympabon.controller.dto;

import com.gympabon.model.Trainer;

/**
 * A trainer as the frontend sees it, with its Premium client slots.
 */
public record TrainerResponse(int id, String documentNumber, String firstName, String lastName, String fullName,
                              String phone, String email, String specialty, String workSchedule, boolean active,
                              int premiumClients, int availableSlots) {

    /**
     * Builds the response from a domain trainer.
     *
     * @param trainer domain trainer loaded with its current Premium memberships
     * @return the response
     */
    public static TrainerResponse from(Trainer trainer) {
        return new TrainerResponse(trainer.getId(), trainer.getDocumentNumber(), trainer.getFirstName(),
                trainer.getLastName(), trainer.getFullName(), trainer.getPhone(), trainer.getEmail(),
                trainer.getSpecialty(), trainer.getWorkSchedule(), trainer.isActive(),
                trainer.getPremiumMemberships().size(), trainer.getAvailableSlots());
    }
}
