package com.gympabon.controller.dto;

import com.gympabon.model.Membership;
import java.time.LocalDate;

/**
 * A membership as the frontend sees it, with the names it shows on screen.
 */
public record MembershipResponse(int id, int clientId, String clientDocument, String clientName, int planId,
                                 String planName, Integer trainerId, String trainerName, String duration,
                                 LocalDate startDate, LocalDate endDate, double enrollmentFee, double totalAmount,
                                 String status, boolean paid, String cancellationReason) {

    /**
     * Builds the response from a domain membership.
     *
     * @param membership domain membership with its client, plan and trainer
     * @return the response
     */
    public static MembershipResponse from(Membership membership) {
        boolean hasTrainer = membership.getTrainer() != null;
        return new MembershipResponse(membership.getId(), membership.getClient().getId(),
                membership.getClient().getDocumentNumber(), membership.getClient().getFullName(),
                membership.getPlan().getId(), membership.getPlan().getName(),
                hasTrainer ? membership.getTrainer().getId() : null,
                hasTrainer ? membership.getTrainer().getFullName() : null,
                membership.getDuration().name(), membership.getStartDate(), membership.getEndDate(),
                membership.getEnrollmentFee(), membership.getTotalAmount(), membership.getStatus().name(),
                membership.isPaid(), membership.getCancellationReason());
    }
}
