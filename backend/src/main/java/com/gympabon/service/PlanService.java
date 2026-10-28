package com.gympabon.service;

import com.gympabon.dao.PlanDAO;
import com.gympabon.exception.BusinessRuleException;
import com.gympabon.exception.NotFoundException;
import com.gympabon.model.Plan;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Business rules for the gym plans and their prices.
 */
@Service
public class PlanService {

    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private final PlanDAO planDAO;

    public PlanService(PlanDAO planDAO) {
        this.planDAO = planDAO;
    }

    /**
     * Returns the three plans with their current prices.
     *
     * @return the plans ordered by price
     */
    public List<Plan> listPlans() {
        return planDAO.findAll();
    }

    /**
     * Finds a plan by its identifier.
     *
     * @param planId identifier of the plan
     * @return the plan
     * @throws NotFoundException if the plan does not exist
     */
    public Plan findById(int planId) {
        Plan plan = planDAO.findById(planId);
        if (plan == null) {
            throw new NotFoundException("El plan no existe");
        }
        return plan;
    }

    /**
     * Changes the monthly price and the description of a plan. Memberships
     * already created keep the value they were sold with.
     *
     * @param planId      identifier of the plan
     * @param newPrice    new monthly price, greater than zero
     * @param description new description
     * @throws NotFoundException     if the plan does not exist
     * @throws BusinessRuleException if the price or the description are not valid
     */
    public void updatePlan(int planId, double newPrice, String description) {
        Plan plan = findById(planId);
        if (newPrice <= 0) {
            throw new BusinessRuleException("El precio mensual debe ser mayor que cero");
        }
        if (description == null || description.isBlank()) {
            throw new BusinessRuleException("La descripción del plan es obligatoria");
        }
        if (description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessRuleException("La descripción no puede tener más de " + MAX_DESCRIPTION_LENGTH
                    + " caracteres");
        }
        plan.setMonthlyPrice(newPrice);
        plan.setDescription(description.trim());
        planDAO.update(plan);
    }
}
