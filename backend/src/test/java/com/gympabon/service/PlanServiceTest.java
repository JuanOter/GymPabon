package com.gympabon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gympabon.exception.BusinessRuleException;
import com.gympabon.exception.NotFoundException;
import com.gympabon.fake.InMemoryPlanDAO;
import com.gympabon.fake.TestData;
import com.gympabon.model.Plan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests of {@link PlanService} (RF06). */
class PlanServiceTest {

    private InMemoryPlanDAO planDAO;
    private PlanService planService;

    @BeforeEach
    void setUp() {
        planDAO = new InMemoryPlanDAO();
        TestData.savePlans(planDAO);
        planService = new PlanService(planDAO);
    }

    @Test
    @DisplayName("Lista los tres planes del gimnasio")
    void listsTheThreePlans() {
        assertEquals(3, planService.listPlans().size());
    }

    @Test
    @DisplayName("Buscar un plan que no existe lanza NotFoundException")
    void findUnknownPlanFails() {
        assertThrows(NotFoundException.class, () -> planService.findById(99));
    }

    @Test
    @DisplayName("Actualiza el precio y la descripción quitando espacios sobrantes")
    void updatesPriceAndDescription() {
        planService.updatePlan(1, 75000, "  Nueva descripción  ");

        Plan plan = planService.findById(1);
        assertEquals(75000, plan.getMonthlyPrice());
        assertEquals("Nueva descripción", plan.getDescription());
    }

    @Test
    @DisplayName("No permite un precio en cero o negativo")
    void rejectsPriceNotPositive() {
        assertThrows(BusinessRuleException.class, () -> planService.updatePlan(1, 0, "Descripción"));
        assertThrows(BusinessRuleException.class, () -> planService.updatePlan(1, -5000, "Descripción"));
        assertEquals(TestData.BASIC_PRICE, planService.findById(1).getMonthlyPrice());
    }

    @Test
    @DisplayName("La descripción es obligatoria y tiene máximo 255 caracteres")
    void validatesDescription() {
        assertThrows(BusinessRuleException.class, () -> planService.updatePlan(2, 90000, "   "));
        assertThrows(BusinessRuleException.class, () -> planService.updatePlan(2, 90000, "x".repeat(256)));
    }

    @Test
    @DisplayName("Actualizar un plan que no existe lanza NotFoundException")
    void updateUnknownPlanFails() {
        assertThrows(NotFoundException.class, () -> planService.updatePlan(99, 90000, "Descripción"));
    }
}
