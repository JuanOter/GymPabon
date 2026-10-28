package com.gympabon.controller.dto;

import com.gympabon.model.MembershipDuration;

/**
 * A duration that can be sold, with its months and discount, for the
 * membership form.
 */
public record DurationResponse(String code, int months, double discountRate) {

    /**
     * Builds the response from the enum value.
     *
     * @param duration duration
     * @return the response
     */
    public static DurationResponse from(MembershipDuration duration) {
        return new DurationResponse(duration.name(), duration.getMonths(), duration.getDiscountRate());
    }
}
