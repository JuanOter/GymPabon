package com.gympabon.controller.dto;

import com.gympabon.model.Payment;
import java.time.LocalDate;

/**
 * A payment as the frontend sees it, with the client, the plan and who registered it.
 */
public record PaymentResponse(int id, int membershipId, String clientDocument, String clientName, String planName,
                              double amount, String method, LocalDate paymentDate, String registeredBy,
                              boolean voided, String voidReason) {

    /**
     * Builds the response from a domain payment.
     *
     * @param payment domain payment with its membership and user
     * @return the response
     */
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getMembership().getId(),
                payment.getMembership().getClient().getDocumentNumber(),
                payment.getMembership().getClient().getFullName(), payment.getMembership().getPlan().getName(),
                payment.getAmount(), payment.getMethod().name(), payment.getPaymentDate(),
                payment.getRegisteredBy().getUsername(), payment.isVoided(), payment.getVoidReason());
    }
}
