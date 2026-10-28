package com.gympabon.controller.dto;

import com.gympabon.model.Membership;
import java.time.LocalDate;

/**
 * An active membership that ends soon, for the main panel.
 */
public record ExpiringMembershipResponse(int membershipId, int clientId, String clientName, String planName,
                                         LocalDate endDate) {

    /**
     * Builds the response from a domain membership.
     *
     * @param membership active membership
     * @return the response
     */
    public static ExpiringMembershipResponse from(Membership membership) {
        return new ExpiringMembershipResponse(membership.getId(), membership.getClient().getId(),
                membership.getClient().getFullName(), membership.getPlan().getName(), membership.getEndDate());
    }
}
