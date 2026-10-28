package com.gympabon.fake;

import com.gympabon.dao.PlanDAO;
import com.gympabon.model.Plan;

/** In-memory {@link PlanDAO} for unit tests. */
public class InMemoryPlanDAO extends InMemoryDAO<Plan> implements PlanDAO {

    @Override
    protected int idOf(Plan plan) {
        return plan.getId();
    }

    @Override
    protected void assignId(Plan plan, int id) {
        plan.setId(id);
    }
}
