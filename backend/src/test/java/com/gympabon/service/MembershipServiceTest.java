package com.gympabon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gympabon.exception.BusinessRuleException;
import com.gympabon.exception.NotFoundException;
import com.gympabon.fake.InMemoryClientDAO;
import com.gympabon.fake.InMemoryMembershipDAO;
import com.gympabon.fake.InMemoryPlanDAO;
import com.gympabon.fake.InMemoryTrainerDAO;
import com.gympabon.fake.TestData;
import com.gympabon.model.Client;
import com.gympabon.model.Membership;
import com.gympabon.model.MembershipDuration;
import com.gympabon.model.MembershipStatus;
import com.gympabon.model.Payment;
import com.gympabon.model.PaymentMethod;
import com.gympabon.model.Role;
import com.gympabon.model.Trainer;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests of {@link MembershipService} (RF07, RF09, RF10, RF11). */
class MembershipServiceTest {

    private static final int BASIC = 1;
    private static final int STANDARD = 2;
    private static final int PREMIUM = 3;

    private final LocalDate today = LocalDate.now();
    private InMemoryMembershipDAO membershipDAO;
    private InMemoryTrainerDAO trainerDAO;
    private MembershipService membershipService;
    private int clientId;
    private int trainerId;

    @BeforeEach
    void setUp() {
        InMemoryClientDAO clientDAO = new InMemoryClientDAO();
        InMemoryPlanDAO planDAO = new InMemoryPlanDAO();
        membershipDAO = new InMemoryMembershipDAO();
        trainerDAO = new InMemoryTrainerDAO();
        TestData.savePlans(planDAO);

        Client client = TestData.client("1065123456");
        clientDAO.save(client);
        clientId = client.getId();
        Client inactive = TestData.client("1065999999");
        inactive.setActive(false);
        clientDAO.save(inactive);
        Trainer trainer = TestData.trainer("77123456");
        trainerDAO.save(trainer);
        trainerId = trainer.getId();

        membershipService = new MembershipService(membershipDAO, clientDAO, planDAO, trainerDAO);
    }

    /** Simulates the payment of a membership, as PaymentService does. */
    private void pay(Membership membership) {
        Payment payment = new Payment(membership, membership.getTotalAmount(), PaymentMethod.CASH, today,
                TestData.user("recepcion", Role.RECEPTIONIST));
        membership.attachPayment(payment);
        membershipDAO.update(membership);
    }

    @Test
    @DisplayName("Primera membresía Básico de 1 mes: precio más matrícula, pendiente de pago")
    void createsFirstBasicMembership() {
        Membership membership = membershipService.createMembership(clientId, BASIC, MembershipDuration.ONE_MONTH,
                today, null);

        assertEquals(20000, membership.getEnrollmentFee());
        assertEquals(TestData.BASIC_PRICE + 20000, membership.getTotalAmount());
        assertEquals(today.plusMonths(1).minusDays(1), membership.getEndDate());
        assertEquals(MembershipStatus.PENDING_PAYMENT, membership.getStatus());
        assertEquals(1, membershipDAO.count());
    }

    @Test
    @DisplayName("Aplica el descuento por duración (3 meses = 5 %)")
    void appliesDurationDiscount() {
        Membership membership = membershipService.createMembership(clientId, STANDARD,
                MembershipDuration.THREE_MONTHS, today, null);

        double expected = Math.round(TestData.STANDARD_PRICE * 3 * 0.95) + 20000;
        assertEquals(expected, membership.getTotalAmount());
    }

    @Test
    @DisplayName("Premium no cobra matrícula y ocupa un cupo del entrenador")
    void premiumUsesTrainerSlot() {
        Membership membership = membershipService.createMembership(clientId, PREMIUM, MembershipDuration.ONE_MONTH,
                today, trainerId);

        assertEquals(0, membership.getEnrollmentFee());
        assertEquals(TestData.PREMIUM_PRICE, membership.getTotalAmount());
        assertEquals(Trainer.MAX_PREMIUM_CLIENTS - 1, trainerDAO.findById(trainerId).getAvailableSlots());
    }

    @Test
    @DisplayName("Premium exige entrenador y los otros planes no lo permiten")
    void validatesTrainerByPlan() {
        assertThrows(BusinessRuleException.class, () -> membershipService.createMembership(clientId, PREMIUM,
                MembershipDuration.ONE_MONTH, today, null));
        assertThrows(BusinessRuleException.class, () -> membershipService.createMembership(clientId, BASIC,
                MembershipDuration.ONE_MONTH, today, trainerId));
        assertThrows(NotFoundException.class, () -> membershipService.createMembership(clientId, PREMIUM,
                MembershipDuration.ONE_MONTH, today, 99));
    }

    @Test
    @DisplayName("No se asigna un entrenador sin cupos")
    void rejectsTrainerWithoutSlots() {
        Trainer trainer = trainerDAO.findById(trainerId);
        for (int i = 0; i < Trainer.MAX_PREMIUM_CLIENTS; i++) {
            trainer.getPremiumMemberships().add(new Membership());
        }

        assertThrows(BusinessRuleException.class, () -> membershipService.createMembership(clientId, PREMIUM,
                MembershipDuration.ONE_MONTH, today, trainerId));
    }

    @Test
    @DisplayName("No permite una fecha de inicio anterior a hoy ni duración vacía")
    void validatesStartDateAndDuration() {
        assertThrows(BusinessRuleException.class, () -> membershipService.createMembership(clientId, BASIC,
                MembershipDuration.ONE_MONTH, today.minusDays(1), null));
        assertThrows(BusinessRuleException.class, () -> membershipService.createMembership(clientId, BASIC,
                null, today, null));
    }

    @Test
    @DisplayName("Un cliente con membresía vigente o pendiente no puede tener otra nueva")
    void rejectsSecondCurrentMembership() {
        membershipService.createMembership(clientId, BASIC, MembershipDuration.ONE_MONTH, today, null);

        assertThrows(BusinessRuleException.class, () -> membershipService.createMembership(clientId, STANDARD,
                MembershipDuration.ONE_MONTH, today, null));
    }

    @Test
    @DisplayName("Cliente inactivo o inexistente no puede comprar membresía")
    void validatesClient() {
        assertThrows(BusinessRuleException.class, () -> membershipService.createMembership(2, BASIC,
                MembershipDuration.ONE_MONTH, today, null));
        assertThrows(NotFoundException.class, () -> membershipService.createMembership(99, BASIC,
                MembershipDuration.ONE_MONTH, today, null));
    }

    @Test
    @DisplayName("La renovación empieza el día siguiente al fin de la actual y no cobra matrícula")
    void renewsAfterCurrentMembership() {
        Membership current = membershipService.createMembership(clientId, BASIC, MembershipDuration.ONE_MONTH,
                today, null);
        pay(current);

        Membership renewal = membershipService.renewMembership(clientId, STANDARD, MembershipDuration.ONE_MONTH,
                null);

        assertEquals(current.getEndDate().plusDays(1), renewal.getStartDate());
        assertEquals(0, renewal.getEnrollmentFee());
        assertEquals(TestData.STANDARD_PRICE, renewal.getTotalAmount());
    }

    @Test
    @DisplayName("No renueva sin membresía activa ni dos veces seguidas")
    void validatesRenewal() {
        assertThrows(BusinessRuleException.class, () -> membershipService.renewMembership(clientId, BASIC,
                MembershipDuration.ONE_MONTH, null));

        Membership current = membershipService.createMembership(clientId, BASIC, MembershipDuration.ONE_MONTH,
                today, null);
        pay(current);
        membershipService.renewMembership(clientId, BASIC, MembershipDuration.ONE_MONTH, null);

        assertThrows(BusinessRuleException.class, () -> membershipService.renewMembership(clientId, BASIC,
                MembershipDuration.ONE_MONTH, null));
    }

    @Test
    @DisplayName("La cotización coincide con el total de la membresía")
    void quoteMatchesTotal() {
        double quote = membershipService.quote(clientId, STANDARD, MembershipDuration.SIX_MONTHS);
        Membership membership = membershipService.createMembership(clientId, STANDARD,
                MembershipDuration.SIX_MONTHS, today, null);

        assertEquals(membership.getTotalAmount(), quote);
    }

    @Test
    @DisplayName("Cancelar exige motivo y libera el cupo del entrenador")
    void cancelReleasesTrainerSlot() {
        Membership membership = membershipService.createMembership(clientId, PREMIUM, MembershipDuration.ONE_MONTH,
                today, trainerId);

        assertThrows(BusinessRuleException.class, () -> membershipService.cancelMembership(membership.getId(), " "));
        membershipService.cancelMembership(membership.getId(), "Se muda de ciudad");

        Membership cancelled = membershipService.findById(membership.getId());
        assertEquals(MembershipStatus.CANCELLED, cancelled.getStatus());
        assertEquals("Se muda de ciudad", cancelled.getCancellationReason());
        assertEquals(Trainer.MAX_PREMIUM_CLIENTS, trainerDAO.findById(trainerId).getAvailableSlots());
    }

    @Test
    @DisplayName("No se cancela una membresía vencida")
    void cannotCancelExpired() {
        Membership membership = membershipService.createMembership(clientId, BASIC, MembershipDuration.ONE_MONTH,
                today, null);
        membership.setStatus(MembershipStatus.EXPIRED);

        assertThrows(BusinessRuleException.class,
                () -> membershipService.cancelMembership(membership.getId(), "Motivo"));
    }

    @Test
    @DisplayName("Al pasar la fecha de fin, la membresía activa queda vencida")
    void refreshesStatuses() {
        Membership membership = membershipService.createMembership(clientId, BASIC, MembershipDuration.ONE_MONTH,
                today, null);
        pay(membership);
        assertEquals(MembershipStatus.ACTIVE, membership.getStatus());

        assertEquals(0, membershipService.refreshStatuses(today));
        assertEquals(1, membershipService.refreshStatuses(membership.getEndDate().plusDays(1)));
        assertEquals(MembershipStatus.EXPIRED, membershipService.findById(membership.getId()).getStatus());
    }

    @Test
    @DisplayName("Una membresía pagada que empieza después queda programada")
    void paidFutureMembershipIsScheduled() {
        Membership membership = membershipService.createMembership(clientId, BASIC, MembershipDuration.ONE_MONTH,
                today.plusDays(5), null);
        pay(membership);

        assertEquals(MembershipStatus.SCHEDULED, membership.getStatus());
        assertNull(membership.getCancellationReason());
    }

    @Test
    @DisplayName("Cambiar de entrenador mueve el cupo al nuevo entrenador")
    void changesTrainer() {
        Trainer other = TestData.trainer("77654321");
        trainerDAO.save(other);
        Membership membership = membershipService.createMembership(clientId, PREMIUM, MembershipDuration.ONE_MONTH,
                today, trainerId);

        membershipService.changeTrainer(membership.getId(), other.getId());

        assertEquals(other.getId(), membershipService.findById(membership.getId()).getTrainer().getId());
        assertEquals(Trainer.MAX_PREMIUM_CLIENTS, trainerDAO.findById(trainerId).getAvailableSlots());
        assertEquals(Trainer.MAX_PREMIUM_CLIENTS - 1, trainerDAO.findById(other.getId()).getAvailableSlots());
    }

    @Test
    @DisplayName("Solo se cambia el entrenador de una Premium vigente, a otro entrenador con cupo")
    void validatesTrainerChange() {
        Membership premium = membershipService.createMembership(clientId, PREMIUM, MembershipDuration.ONE_MONTH,
                today, trainerId);
        Trainer full = TestData.trainer("77999999");
        trainerDAO.save(full);
        for (int i = 0; i < Trainer.MAX_PREMIUM_CLIENTS; i++) {
            full.getPremiumMemberships().add(new Membership());
        }

        assertThrows(BusinessRuleException.class, () -> membershipService.changeTrainer(premium.getId(), trainerId));
        assertThrows(BusinessRuleException.class,
                () -> membershipService.changeTrainer(premium.getId(), full.getId()));
        assertThrows(NotFoundException.class, () -> membershipService.changeTrainer(premium.getId(), 99));
        assertEquals(trainerId, premium.getTrainer().getId());

        membershipService.cancelMembership(premium.getId(), "Cambio de ciudad");
        Trainer other = TestData.trainer("77654321");
        trainerDAO.save(other);
        assertThrows(BusinessRuleException.class,
                () -> membershipService.changeTrainer(premium.getId(), other.getId()));
    }

    @Test
    @DisplayName("Un plan que no es Premium no tiene entrenador que cambiar")
    void basicMembershipHasNoTrainer() {
        Membership basic = membershipService.createMembership(clientId, BASIC, MembershipDuration.ONE_MONTH,
                today, null);

        assertThrows(BusinessRuleException.class, () -> membershipService.changeTrainer(basic.getId(), trainerId));
        assertEquals(1, membershipService.listAll().size());
    }

    @Test
    @DisplayName("Buscar una membresía que no existe lanza NotFoundException")
    void findUnknownMembershipFails() {
        assertThrows(NotFoundException.class, () -> membershipService.findById(99));
    }
}
