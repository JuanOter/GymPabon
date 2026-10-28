package com.gympabon.model;

import com.gympabon.exception.BusinessRuleException;
import java.time.LocalDate;

/**
 * Full payment of a membership. A payment is never deleted; it can only be
 * voided with a reason, so its history is kept.
 */
public class Payment {

    private int id;
    private Membership membership;
    private double amount;
    private PaymentMethod method;
    private LocalDate paymentDate;
    private User registeredBy;
    private boolean voided;
    private String voidReason;

    /** Empty constructor used when a payment is loaded from the database. */
    public Payment() {
    }

    public Payment(Membership membership, double amount, PaymentMethod method, LocalDate paymentDate,
                   User registeredBy) {
        this.membership = membership;
        this.amount = amount;
        this.method = method;
        this.paymentDate = paymentDate;
        this.registeredBy = registeredBy;
    }

    /**
     * Tells whether the payment is still valid, that is, it was not voided.
     *
     * @return true if the payment was not voided
     */
    public boolean isValid() {
        return !voided;
    }

    /**
     * Voids the payment and keeps the reason.
     *
     * @param reason why the payment is voided
     * @throws BusinessRuleException if the reason is empty or the payment was
     *                               already voided
     */
    public void voidPayment(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("El motivo de la anulación es obligatorio");
        }
        if (voided) {
            throw new BusinessRuleException("El pago ya estaba anulado");
        }
        voided = true;
        voidReason = reason.trim();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Membership getMembership() {
        return membership;
    }

    public void setMembership(Membership membership) {
        this.membership = membership;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public User getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(User registeredBy) {
        this.registeredBy = registeredBy;
    }

    public boolean isVoided() {
        return voided;
    }

    public void setVoided(boolean voided) {
        this.voided = voided;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void setVoidReason(String voidReason) {
        this.voidReason = voidReason;
    }
}
