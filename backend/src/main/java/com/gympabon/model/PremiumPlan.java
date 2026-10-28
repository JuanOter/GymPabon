package com.gympabon.model;

import java.time.LocalDateTime;

/**
 * Premium plan ($140.000 per month): access during all the gym opening hours,
 * a personal trainer and no enrollment fee.
 */
public class PremiumPlan extends Plan {

    public PremiumPlan() {
    }

    public PremiumPlan(int id, String name, double monthlyPrice, String description) {
        super(id, name, monthlyPrice, description);
    }

    @Override
    public boolean canAccess(LocalDateTime dateTime) {
        return isWithinGymHours(dateTime);
    }

    @Override
    public double getEnrollmentFee() {
        return 0;
    }

    @Override
    public boolean includesTrainer() {
        return true;
    }
}
