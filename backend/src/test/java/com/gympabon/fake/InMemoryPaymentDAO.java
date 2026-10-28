package com.gympabon.fake;

import com.gympabon.dao.PaymentDAO;
import com.gympabon.model.Payment;
import com.gympabon.model.PaymentMethod;
import java.time.LocalDate;
import java.util.List;

/** In-memory {@link PaymentDAO} for unit tests. */
public class InMemoryPaymentDAO extends InMemoryDAO<Payment> implements PaymentDAO {

    @Override
    protected int idOf(Payment payment) {
        return payment.getId();
    }

    @Override
    protected void assignId(Payment payment, int id) {
        payment.setId(id);
    }

    @Override
    public List<Payment> findByMembership(int membershipId) {
        return rows.values().stream().filter(p -> p.getMembership().getId() == membershipId).toList();
    }

    @Override
    public List<Payment> findByDateRange(LocalDate from, LocalDate to, PaymentMethod method) {
        return rows.values().stream()
                .filter(p -> !p.getPaymentDate().isBefore(from) && !p.getPaymentDate().isAfter(to))
                .filter(p -> method == null || p.getMethod() == method)
                .toList();
    }
}
