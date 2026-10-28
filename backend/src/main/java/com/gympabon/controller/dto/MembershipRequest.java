package com.gympabon.controller.dto;

import com.gympabon.model.MembershipDuration;
import java.time.LocalDate;

/**
 * Data of a new membership. trainerId is only sent for Premium plans.
 */
public record MembershipRequest(int clientId, int planId, MembershipDuration duration, LocalDate startDate,
                                Integer trainerId) {
}
