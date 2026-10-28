package com.gympabon.controller.dto;

/**
 * New price and description of a plan, sent by the administrator.
 */
public record PlanUpdateRequest(double monthlyPrice, String description) {
}
