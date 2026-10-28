package com.gympabon.model;

/**
 * Durations that can be sold and the discount of each one, kept in a single
 * place of the code.
 */
public enum MembershipDuration {
    ONE_MONTH(1, 0.0),
    THREE_MONTHS(3, 0.05),
    SIX_MONTHS(6, 0.10),
    TWELVE_MONTHS(12, 0.15);

    private final int months;
    private final double discountRate;

    MembershipDuration(int months, double discountRate) {
        this.months = months;
        this.discountRate = discountRate;
    }

    public int getMonths() {
        return months;
    }

    public double getDiscountRate() {
        return discountRate;
    }
}
