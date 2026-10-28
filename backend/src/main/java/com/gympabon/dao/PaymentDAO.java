package com.gympabon.dao;

import com.gympabon.model.Payment;
import com.gympabon.model.PaymentMethod;
import java.time.LocalDate;
import java.util.List;

/**
 * Data access for payments. Payments are voided instead of deleted, so their
 * history is always kept.
 */
public interface PaymentDAO extends GenericDAO<Payment, Integer> {

    /**
     * Returns every payment of a membership, voided ones included.
     *
     * @param membershipId identifier of the membership
     * @return the payments, newest first
     */
    List<Payment> findByMembership(int membershipId);

    /**
     * Returns the payments made between two dates, both included.
     *
     * @param from   first day of the period
     * @param to     last day of the period
     * @param method payment method to filter by, or null for all methods
     * @return the payments of the period, newest first
     */
    List<Payment> findByDateRange(LocalDate from, LocalDate to, PaymentMethod method);
}
