package com.gympabon.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the plan hierarchy: every subclass answers {@code canAccess}
 * differently (polymorphism) and the price rules are shared (inheritance).
 */
class PlanAccessTest {

    private static final LocalDateTime MONDAY_10_AM = LocalDateTime.of(2026, 11, 2, 10, 0);
    private static final LocalDateTime MONDAY_4_PM = LocalDateTime.of(2026, 11, 2, 16, 0);
    private static final LocalDateTime MONDAY_11_PM = LocalDateTime.of(2026, 11, 2, 23, 0);
    private static final LocalDateTime SATURDAY_10_AM = LocalDateTime.of(2026, 11, 7, 10, 0);
    private static final LocalDateTime SUNDAY_2_PM = LocalDateTime.of(2026, 11, 8, 14, 0);

    private final Plan basic = new BasicPlan(1, "Básico", 70000, "");
    private final Plan standard = new StandardPlan(2, "Estándar", 95000, "");
    private final Plan premium = new PremiumPlan(3, "Premium", 140000, "");

    @Test
    @DisplayName("Básico: solo de lunes a viernes de 5:00 a. m. a 3:00 p. m.")
    void basicPlanHours() {
        assertTrue(basic.canAccess(MONDAY_10_AM));
        assertFalse(basic.canAccess(MONDAY_4_PM));
        assertFalse(basic.canAccess(SATURDAY_10_AM));
    }

    @Test
    @DisplayName("Estándar y Premium: todo el horario del gimnasio")
    void fullHoursPlans() {
        for (Plan plan : List.of(standard, premium)) {
            assertTrue(plan.canAccess(MONDAY_4_PM));
            assertTrue(plan.canAccess(SATURDAY_10_AM));
            assertFalse(plan.canAccess(MONDAY_11_PM));
            assertFalse(plan.canAccess(SUNDAY_2_PM));
        }
    }

    @Test
    @DisplayName("Descuentos por duración: 0 %, 5 %, 10 % y 15 %")
    void durationDiscounts() {
        assertEquals(95000, standard.calculatePrice(MembershipDuration.ONE_MONTH));
        assertEquals(270750, standard.calculatePrice(MembershipDuration.THREE_MONTHS));
        assertEquals(513000, standard.calculatePrice(MembershipDuration.SIX_MONTHS));
        assertEquals(969000, standard.calculatePrice(MembershipDuration.TWELVE_MONTHS));
    }

    @Test
    @DisplayName("Solo Premium incluye entrenador y no cobra matrícula")
    void premiumRules() {
        assertTrue(premium.includesTrainer());
        assertFalse(basic.includesTrainer());
        assertEquals(0, premium.getEnrollmentFee());
        assertEquals(Plan.ENROLLMENT_FEE, basic.getEnrollmentFee());
        assertEquals(Plan.ENROLLMENT_FEE, standard.getEnrollmentFee());
    }
}
