package com.gympabon.controller.dto;

import com.gympabon.model.MembershipDuration;

/**
 * Data of a renewal; its start date is calculated by the system.
 */
public record RenewalRequest(int clientId, int planId, MembershipDuration duration, Integer trainerId) {
}
