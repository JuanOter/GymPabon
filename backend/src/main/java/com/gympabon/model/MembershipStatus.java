package com.gympabon.model;

/**
 * States that a membership goes through from its sale to its end.
 */
public enum MembershipStatus {
    PENDING_PAYMENT,
    SCHEDULED,
    ACTIVE,
    EXPIRED,
    CANCELLED
}
