package com.gympabon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gympabon.exception.BusinessRuleException;
import com.gympabon.exception.NotFoundException;
import com.gympabon.fake.InMemoryMembershipDAO;
import com.gympabon.fake.InMemoryPaymentDAO;
import com.gympabon.fake.TestData;
import com.gympabon.model.Client;
import com.gympabon.model.Membership;
import com.gympabon.model.MembershipDuration;
import com.gympabon.model.MembershipStatus;
import com.gympabon.model.Payment;
import com.gympabon.model.PaymentMethod;
import com.gympabon.model.Role;
import com.gympabon.model.User;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests of {@link PaymentService} (RF08, RF14). */
class PaymentServiceTest {

    private final LocalDate today = LocalDate.now();
    private final User receptionist = TestData.user("recepcion", Role.RECEPTIONIST);
    private final User admin = TestData.user("admin", Role.ADMIN);
    private InMemoryMembershipDAO membershipDAO;
    private InMemoryPaymentDAO paymentDAO;
    private PaymentService paymentService;
    private Client client;

    @BeforeEach
    void setUp() {
        membershipDAO = new InMemoryMembershipDAO();
        paymentDAO = new InMemoryPaymentDAO();
        paymentService = new PaymentService(paymentDAO, membershipDAO);
        client = TestData.client("1065123456");
        client.setId(1);
    }

    private Membership pendingMembership(LocalDate startDate) {
        Membership membership = new Membership(client, TestData.standardPlan(), null,
                MembershipDuration.ONE_MONTH, startDate);
        membershipDAO.save(membership);
        return membership;
    }

    @Test
    @DisplayName("Registra el pago por el total y la membresía queda activa")
    void registersPayment() {
        Membership membership = pendingMembership(today);

        Payment payment = paymentService.registerPayment(membership.getId(), PaymentMethod.TRANSFER, receptionist);

        assertEquals(TestData.STANDARD_PRICE + 20000, payment.getAmount());
        assertEquals(today, payment.getPaymentDate());
        assertEquals(MembershipStatus.ACTIVE, membershipDAO.findById(membership.getId()).getStatus());
        assertEquals(1, paymentDAO.count());
    }

    @Test
    @DisplayName("Si la membresía empieza después, queda programada")
    void futureMembershipIsScheduled() {
        Membership membership = pendingMembership(today.plusDays(3));

        paymentService.registerPayment(membership.getId(), PaymentMethod.CASH, receptionist);

        assertEquals(MembershipStatus.SCHEDULED, membership.getStatus());
    }

    @Test
    @DisplayName("No cobra dos veces ni sin método o usuario")
    void validatesPayment() {
        Membership membership = pendingMembership(today);

        assertThrows(BusinessRuleException.class,
                () -> paymentService.registerPayment(membership.getId(), null, receptionist));
        assertThrows(BusinessRuleException.class,
                () -> paymentService.registerPayment(membership.getId(), PaymentMethod.CASH, null));
        paymentService.registerPayment(membership.getId(), PaymentMethod.CASH, receptionist);
        assertThrows(BusinessRuleException.class,
                () -> paymentService.registerPayment(membership.getId(), PaymentMethod.CASH, receptionist));
        assertThrows(NotFoundException.class,
                () -> paymentService.registerPayment(99, PaymentMethod.CASH, receptionist));
        assertEquals(1, paymentDAO.count());
    }

    @Test
    @DisplayName("Solo el administrador anula pagos y debe dar un motivo")
    void onlyAdminVoidsWithReason() {
        Membership membership = pendingMembership(today);
        Payment payment = paymentService.registerPayment(membership.getId(), PaymentMethod.CASH, receptionist);

        assertThrows(BusinessRuleException.class,
                () -> paymentService.voidPayment(payment.getId(), "Error de digitación", receptionist));
        assertThrows(BusinessRuleException.class, () -> paymentService.voidPayment(payment.getId(), " ", admin));
        assertThrows(NotFoundException.class, () -> paymentService.voidPayment(99, "Motivo", admin));
        assertTrue(payment.isValid());
    }

    @Test
    @DisplayName("Al anular el pago, la membresía vuelve a pendiente de pago")
    void voidingReturnsMembershipToPending() {
        Membership membership = pendingMembership(today);
        Payment payment = paymentService.registerPayment(membership.getId(), PaymentMethod.CASH, receptionist);

        paymentService.voidPayment(payment.getId(), "Pago registrado por error", admin);

        assertTrue(payment.isVoided());
        assertEquals("Pago registrado por error", payment.getVoidReason());
        assertEquals(MembershipStatus.PENDING_PAYMENT, membershipDAO.findById(membership.getId()).getStatus());
        assertThrows(BusinessRuleException.class,
                () -> paymentService.voidPayment(payment.getId(), "Otra vez", admin));
    }

    @Test
    @DisplayName("Lista los pagos por periodo y método, y valida las fechas")
    void listsPaymentsByPeriod() {
        paymentService.registerPayment(pendingMembership(today).getId(), PaymentMethod.CASH, receptionist);
        paymentService.registerPayment(pendingMembership(today).getId(), PaymentMethod.TRANSFER, receptionist);

        assertEquals(2, paymentService.listPayments(today, today, null).size());
        assertEquals(1, paymentService.listPayments(today, today, PaymentMethod.TRANSFER).size());
        assertEquals(0, paymentService.listPayments(today.minusDays(5), today.minusDays(1), null).size());
        assertThrows(BusinessRuleException.class, () -> paymentService.listPayments(today, today.minusDays(1), null));
        assertThrows(BusinessRuleException.class, () -> paymentService.listPayments(null, today, null));
    }

    @Test
    @DisplayName("Los pendientes de pago son las membresías sin pagar")
    void listsPendingMemberships() {
        pendingMembership(today);
        Membership paid = pendingMembership(today);
        paymentService.registerPayment(paid.getId(), PaymentMethod.CASH, receptionist);

        assertEquals(1, paymentService.listPendingMemberships().size());
    }
}
