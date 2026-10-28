package com.gympabon.dao;

import com.gympabon.model.Plan;

/**
 * Data access for the gym plans. The three plans are created by the database
 * script; the system only reads them and updates their price and description.
 */
public interface PlanDAO extends GenericDAO<Plan, Integer> {
}
