package com.gympabon.model;

import java.time.LocalDateTime;

/**
 * Standard plan ($95.000 per month): access during all the gym opening hours
 * and a $20.000 enrollment fee.
 */
public class StandardPlan extends Plan {

    public StandardPlan() {
    }

    public StandardPlan(int id, String name, double monthlyPrice, String description) {
        super(id, name, monthlyPrice, description);
    }

    @Override
    public boolean canAccess(LocalDateTime dateTime) {
        return isWithinGymHours(dateTime);
    }
}
