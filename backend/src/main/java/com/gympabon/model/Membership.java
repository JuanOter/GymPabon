package com.gympabon.model;

import com.gympabon.exception.BusinessRuleException;
import java.time.LocalDate;

/**
 * Purchase of a plan by a client for a period of time. It is the expert in its
 * own price, dates and status.
 */
public class Membership {

    private int id;
    private Client client;
    private Plan plan;
    private Trainer trainer;
    private MembershipDuration duration;
    private LocalDate startDate;
    private LocalDate endDate;
    private double enrollmentFee;
    private double totalAmount;
    private MembershipStatus status = MembershipStatus.PENDING_PAYMENT;
    private Payment payment;
    private String cancellationReason;

    /** Empty constructor used when a membership is loaded from the database. */
    public Membership() {
    }

    /**
     * Creates a new membership pending payment. The enrollment fee is only
     * charged on the client's first paid membership and only if the plan
     * charges it. The end date and the total are calculated here.
     *
     * @param client    client who buys the membership
     * @param plan      plan bought
     * @param trainer   assigned trainer for Premium plans, otherwise null
     * @param duration  duration bought
     * @param startDate first day of the membership
     */
    public Membership(Client client, Plan plan, Trainer trainer, MembershipDuration duration, LocalDate startDate) {
        this.client = client;
        this.plan = plan;
        this.trainer = trainer;
        this.duration = duration;
        this.startDate = startDate;
        this.enrollmentFee = client.hasPreviousMemberships() ? 0 : plan.getEnrollmentFee();
        this.endDate = calculateEndDate();
        this.totalAmount = calculateTotal();
    }

    /**
     * Calculates the total to pay: the plan price for the duration plus the
     * enrollment fee.
     *
     * @return the total in pesos
     */
    public final double calculateTotal() {
        return plan.calculatePrice(duration) + enrollmentFee;
    }

    /**
     * Calculates the last day of the membership: start date plus the months
     * of the duration, minus one day.
     *
     * @return the end date
     */
    public final LocalDate calculateEndDate() {
        return startDate.plusMonths(duration.getMonths()).minusDays(1);
    }

    /**
     * Links the payment to this membership and updates the status using the
     * payment date.
     *
     * @param payment payment of the membership
     */
    public void attachPayment(Payment payment) {
        this.payment = payment;
        updateStatus(payment.getPaymentDate());
    }

    /**
     * Tells whether the membership has a payment that was not voided.
     *
     * @return true if it is paid
     */
    public boolean isPaid() {
        return payment != null && payment.isValid();
    }

    /**
     * Recalculates the status for the given day: pending payment if it has no
     * valid payment, scheduled if it has not started, active during its period
     * and expired after it ends. A cancelled membership does not change.
     *
     * @param today reference date
     */
    public void updateStatus(LocalDate today) {
        if (status == MembershipStatus.CANCELLED) {
            return;
        }
        if (!isPaid()) {
            status = MembershipStatus.PENDING_PAYMENT;
        } else if (today.isBefore(startDate)) {
            status = MembershipStatus.SCHEDULED;
        } else if (today.isAfter(endDate)) {
            status = MembershipStatus.EXPIRED;
        } else {
            status = MembershipStatus.ACTIVE;
        }
    }

    /**
     * Tells whether the membership is active on the given date: it is paid,
     * not cancelled and the date is inside its period.
     *
     * @param date date to check
     * @return true if the membership is active on that date
     */
    public boolean isActiveOn(LocalDate date) {
        return status != MembershipStatus.CANCELLED
                && isPaid()
                && !date.isBefore(startDate)
                && !date.isAfter(endDate);
    }

    /**
     * Cancels the membership and frees the trainer slot, if there is one.
     *
     * @param reason why the membership is cancelled
     * @throws BusinessRuleException if the reason is empty or the membership
     *                               is already expired or cancelled
     */
    public void cancel(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("El motivo de la cancelación es obligatorio");
        }
        if (status == MembershipStatus.EXPIRED || status == MembershipStatus.CANCELLED) {
            throw new BusinessRuleException("La membresía ya está vencida o cancelada");
        }
        status = MembershipStatus.CANCELLED;
        cancellationReason = reason.trim();
        if (trainer != null) {
            trainer.releaseMembership(this);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public Trainer getTrainer() {
        return trainer;
    }

    public void setTrainer(Trainer trainer) {
        this.trainer = trainer;
    }

    public MembershipDuration getDuration() {
        return duration;
    }

    public void setDuration(MembershipDuration duration) {
        this.duration = duration;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public double getEnrollmentFee() {
        return enrollmentFee;
    }

    public void setEnrollmentFee(double enrollmentFee) {
        this.enrollmentFee = enrollmentFee;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public void setStatus(MembershipStatus status) {
        this.status = status;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
