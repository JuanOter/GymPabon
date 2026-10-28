package com.gympabon.controller.dto;

import com.gympabon.model.PaymentMethod;

/**
 * Payment of a membership pending payment; the amount is its total.
 */
public record PaymentRequest(int membershipId, PaymentMethod method) {
}
