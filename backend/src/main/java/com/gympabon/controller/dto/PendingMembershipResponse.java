package com.gympabon.controller.dto;

import com.gympabon.model.Membership;
import java.time.LocalDate;

/**
 * A membership waiting for its payment, as listed on the payments screen.
 */
public record PendingMembershipResponse(int membershipId, String clientDocument, String clientName,
                                        String planName, LocalDate startDate, LocalDate endDate,
                                        double totalAmount) {

    /**
     * Builds the response from a domain membership.
     *
     * @param membership membership pending payment
     * @return the response
     */
    public static PendingMembershipResponse from(Membership membership) {
        return new PendingMembershipResponse(membership.getId(), membership.getClient().getDocumentNumber(),
                membership.getClient().getFullName(), membership.getPlan().getName(), membership.getStartDate(),
                membership.getEndDate(), membership.getTotalAmount());
    }
}
