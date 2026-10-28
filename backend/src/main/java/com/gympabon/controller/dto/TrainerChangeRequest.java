package com.gympabon.controller.dto;

/**
 * Data to assign another trainer to a Premium membership.
 *
 * @param trainerId identifier of the new trainer
 */
public record TrainerChangeRequest(int trainerId) {
}
