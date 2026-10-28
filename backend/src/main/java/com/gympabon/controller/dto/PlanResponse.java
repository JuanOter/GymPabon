package com.gympabon.controller.dto;

import com.gympabon.model.BasicPlan;
import com.gympabon.model.Plan;
import com.gympabon.model.PremiumPlan;

/**
 * A plan as the frontend sees it, with the rules that change between plans.
 */
public record PlanResponse(int id, String type, String name, double monthlyPrice, String description,
                           double enrollmentFee, boolean includesTrainer) {

    /**
     * Builds the response from a domain plan; the enrollment fee and the
     * trainer come from the plan's own (polymorphic) methods.
     *
     * @param plan domain plan
     * @return the response
     */
    public static PlanResponse from(Plan plan) {
        return new PlanResponse(plan.getId(), typeOf(plan), plan.getName(), plan.getMonthlyPrice(),
                plan.getDescription(), plan.getEnrollmentFee(), plan.includesTrainer());
    }

    private static String typeOf(Plan plan) {
        if (plan instanceof BasicPlan) {
            return "BASIC";
        }
        if (plan instanceof PremiumPlan) {
            return "PREMIUM";
        }
        return "STANDARD";
    }
}
