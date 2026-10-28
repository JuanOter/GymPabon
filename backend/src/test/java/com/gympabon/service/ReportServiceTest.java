package com.gympabon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gympabon.exception.BusinessRuleException;
import com.gympabon.fake.InMemoryAttendanceDAO;
import com.gympabon.fake.InMemoryClientDAO;
import com.gympabon.fake.InMemoryMembershipDAO;
import com.gympabon.fake.InMemoryPaymentDAO;
import com.gympabon.fake.TestData;
import com.gympabon.model.Attendance;
import com.gympabon.model.Client;
import com.gympabon.model.Membership;
import com.gympabon.model.MembershipDuration;
import com.gympabon.model.MembershipStatus;
import com.gympabon.model.Payment;
import com.gympabon.model.PaymentMethod;
import com.gympabon.model.Plan;
import com.gympabon.model.Role;
import com.gympabon.model.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests of {@link ReportService} (RF15). */
class ReportServiceTest {

    private final LocalDate today = LocalDate.now();
    private final User receptionist = TestData.user("recepcion", Role.RECEPTIONIST);
    private InMemoryClientDAO clientDAO;
    private InMemoryMembershipDAO membershipDAO;
    private InMemoryPaymentDAO paymentDAO;
    private InMemoryAttendanceDAO attendanceDAO;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        clientDAO = new InMemoryClientDAO();
        membershipDAO = new InMemoryMembershipDAO();
        paymentDAO = new InMemoryPaymentDAO();
        attendanceDAO = new InMemoryAttendanceDAO();
        reportService = new ReportService(clientDAO, membershipDAO, paymentDAO, attendanceDAO);
    }

    /** Saves a client with an active, paid membership that ends on the given day. */
    private Client activeClient(String document, Plan plan, LocalDate endDate) {
        Client client = TestData.client(document);
        clientDAO.save(client);
        Membership membership = new Membership(client, plan, null, MembershipDuration.ONE_MONTH,
                endDate.minusMonths(1).plusDays(1));
        membershipDAO.save(membership);
        Payment payment = new Payment(membership, membership.getTotalAmount(), PaymentMethod.CASH, today,
                receptionist);
        paymentDAO.save(payment);
        membership.attachPayment(payment);
        membership.setStatus(MembershipStatus.ACTIVE);
        return client;
    }

    private void entry(Client client, LocalDate day, boolean allowed) {
        attendanceDAO.save(allowed
                ? Attendance.createAllowed(client, day.atTime(9, 0), receptionist)
                : Attendance.createRejected(client, day.atTime(9, 0), receptionist, "Sin membresía"));
    }

    @Test
    @DisplayName("Resumen: clientes activos, ingresos del mes, entradas de hoy y por vencer")
    void buildsDashboardSummary() {
        Client first = activeClient("1065000001", TestData.basicPlan(), today.plusDays(3));
        activeClient("1065000002", TestData.standardPlan(), today.plusDays(20));
        clientDAO.save(TestData.client("1065000003"));
        paymentDAO.findAll().get(1).voidPayment("Error");
        entry(first, today, true);
        entry(first, today, false);

        Map<String, Object> summary = reportService.getDashboardSummary(today);

        assertEquals(2, summary.get("activeClients"));
        assertEquals(TestData.BASIC_PRICE + 20000, summary.get("monthlyIncome"));
        assertEquals(1L, summary.get("todayEntries"));
        assertEquals(1, summary.get("expiringMemberships"));
        assertEquals(3, summary.get("registeredClients"));
    }

    @Test
    @DisplayName("Por vencer: solo las activas que terminan en los próximos días, ordenadas")
    void listsExpiringMemberships() {
        activeClient("1065000001", TestData.basicPlan(), today.plusDays(6));
        activeClient("1065000002", TestData.basicPlan(), today.plusDays(1));
        activeClient("1065000003", TestData.basicPlan(), today.plusDays(15));

        List<Membership> expiring = reportService.getExpiringMemberships(7);

        assertEquals(2, expiring.size());
        assertEquals(today.plusDays(1), expiring.get(0).getEndDate());
        assertThrows(BusinessRuleException.class, () -> reportService.getExpiringMemberships(-1));
    }

    @Test
    @DisplayName("Ingresos y clientes agrupados por plan, sin pagos anulados")
    void groupsByPlan() {
        activeClient("1065000001", TestData.basicPlan(), today.plusDays(10));
        activeClient("1065000002", TestData.premiumPlan(), today.plusDays(10));
        activeClient("1065000003", TestData.premiumPlan(), today.plusDays(10));
        paymentDAO.findAll().get(2).voidPayment("Error");

        Map<String, Double> income = reportService.getIncomeByPlan(today.withDayOfMonth(1), today);
        Map<String, Integer> clients = reportService.getClientsByPlan();

        assertEquals(TestData.BASIC_PRICE + 20000, income.get("Básico"));
        assertEquals(TestData.PREMIUM_PRICE, income.get("Premium"));
        assertEquals(1, clients.get("Básico"));
        assertEquals(2, clients.get("Premium"));
    }

    @Test
    @DisplayName("Asistencia por día cuenta solo las entradas permitidas")
    void countsAttendanceByDay() {
        Client client = activeClient("1065000001", TestData.standardPlan(), today.plusDays(10));
        entry(client, today.minusDays(1), true);
        entry(client, today, true);
        entry(client, today, true);
        entry(client, today, false);

        Map<LocalDate, Integer> byDay = reportService.getAttendanceByDay(today.minusDays(6), today);

        assertEquals(1, byDay.get(today.minusDays(1)));
        assertEquals(2, byDay.get(today));
        assertThrows(BusinessRuleException.class, () -> reportService.getAttendanceByDay(today, today.minusDays(1)));
        assertThrows(BusinessRuleException.class, () -> reportService.getIncomeByPlan(null, today));
    }
}
