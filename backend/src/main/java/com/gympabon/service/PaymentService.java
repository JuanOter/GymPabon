package com.gympabon.service;

import com.gympabon.dao.MembershipDAO;
import com.gympabon.dao.PaymentDAO;
import com.gympabon.exception.BusinessRuleException;
import com.gympabon.exception.NotFoundException;
import com.gympabon.model.Membership;
import com.gympabon.model.MembershipStatus;
import com.gympabon.model.Payment;
import com.gympabon.model.PaymentMethod;
import com.gympabon.model.User;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Business rules for collecting and voiding membership payments.
 */
@Service
public class PaymentService {

    private final PaymentDAO paymentDAO;
    private final MembershipDAO membershipDAO;

    public PaymentService(PaymentDAO paymentDAO, MembershipDAO membershipDAO) {
        this.paymentDAO = paymentDAO;
        this.membershipDAO = membershipDAO;
    }

    /**
     * Returns the memberships waiting for their payment.
     *
     * @return the memberships pending payment
     */
    public List<Membership> listPendingMemberships() {
        return membershipDAO.findByStatus(MembershipStatus.PENDING_PAYMENT);
    }

    /**
     * Registers the full payment of a membership pending payment. The
     * membership becomes active, or scheduled if it starts later.
     *
     * @param membershipId identifier of the membership
     * @param method       payment method
     * @param registeredBy user who receives the payment
     * @return the saved payment
     * @throws NotFoundException     if the membership does not exist
     * @throws BusinessRuleException if the membership is not pending payment or data is missing
     */
    public Payment registerPayment(int membershipId, PaymentMethod method, User registeredBy) {
        Membership membership = findMembership(membershipId);
        if (membership.getStatus() != MembershipStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("La membresía no está pendiente de pago");
        }
        if (method == null) {
            throw new BusinessRuleException("El método de pago es obligatorio");
        }
        if (registeredBy == null) {
            throw new BusinessRuleException("Debe indicar el usuario que registra el pago");
        }
        Payment payment = new Payment(membership, membership.getTotalAmount(), method, LocalDate.now(), registeredBy);
        paymentDAO.save(payment);
        membership.attachPayment(payment);
        membershipDAO.update(membership);
        return payment;
    }

    /**
     * Voids a payment with its reason; the membership goes back to pending
     * payment. Only the administrator can do it.
     *
     * @param paymentId identifier of the payment
     * @param reason    why it is voided
     * @param admin     administrator who voids it
     * @throws NotFoundException     if the payment does not exist
     * @throws BusinessRuleException if the user is not an administrator, the
     *                               reason is empty or the payment was already voided
     */
    public void voidPayment(int paymentId, String reason, User admin) {
        if (admin == null || !admin.isAdmin()) {
            throw new BusinessRuleException("Solo el administrador puede anular pagos");
        }
        Payment payment = paymentDAO.findById(paymentId);
        if (payment == null) {
            throw new NotFoundException("El pago no existe");
        }
        payment.voidPayment(reason);
        paymentDAO.update(payment);

        Membership membership = findMembership(payment.getMembership().getId());
        membership.updateStatus(LocalDate.now());
        membershipDAO.update(membership);
    }

    /**
     * Returns the payments of a period, optionally filtered by method.
     *
     * @param from   first day of the period
     * @param to     last day of the period
     * @param method payment method, or null for all
     * @return the payments of the period
     * @throws BusinessRuleException if the dates are missing or reversed
     */
    public List<Payment> listPayments(LocalDate from, LocalDate to, PaymentMethod method) {
        if (from == null || to == null) {
            throw new BusinessRuleException("Debe indicar la fecha inicial y la final");
        }
        if (from.isAfter(to)) {
            throw new BusinessRuleException("La fecha inicial no puede ser posterior a la final");
        }
        return paymentDAO.findByDateRange(from, to, method);
    }

    private Membership findMembership(int membershipId) {
        Membership membership = membershipDAO.findById(membershipId);
        if (membership == null) {
            throw new NotFoundException("La membresía no existe");
        }
        return membership;
    }
}
