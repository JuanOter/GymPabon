package com.gympabon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gympabon.exception.BusinessRuleException;
import com.gympabon.exception.NotFoundException;
import com.gympabon.fake.InMemoryAttendanceDAO;
import com.gympabon.fake.InMemoryClientDAO;
import com.gympabon.fake.InMemoryMembershipDAO;
import com.gympabon.fake.TestData;
import com.gympabon.model.Attendance;
import com.gympabon.model.Client;
import com.gympabon.model.Membership;
import com.gympabon.model.MembershipDuration;
import com.gympabon.model.Payment;
import com.gympabon.model.PaymentMethod;
import com.gympabon.model.Plan;
import com.gympabon.model.Role;
import com.gympabon.model.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests of {@link AttendanceService} (RF12, RF13). */
class AttendanceServiceTest {

    private static final String DOCUMENT = "1065123456";

    /** A Monday in the future, so the plan hours do not depend on the day the tests run. */
    private final LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    private final User receptionist = TestData.user("recepcion", Role.RECEPTIONIST);
    private InMemoryClientDAO clientDAO;
    private InMemoryMembershipDAO membershipDAO;
    private InMemoryAttendanceDAO attendanceDAO;
    private AttendanceService attendanceService;
    private Client client;

    @BeforeEach
    void setUp() {
        clientDAO = new InMemoryClientDAO();
        membershipDAO = new InMemoryMembershipDAO();
        attendanceDAO = new InMemoryAttendanceDAO();
        attendanceService = new AttendanceService(attendanceDAO, clientDAO, membershipDAO);
        client = TestData.client(DOCUMENT);
        clientDAO.save(client);
    }

    /** Saves a membership of the client; when paid, its status is updated for the given day. */
    private Membership membership(Plan plan, LocalDate start, boolean paid) {
        Membership membership = new Membership(client, plan, null, MembershipDuration.ONE_MONTH, start);
        membershipDAO.save(membership);
        if (paid) {
            membership.attachPayment(new Payment(membership, membership.getTotalAmount(), PaymentMethod.CASH,
                    start, receptionist));
            membership.updateStatus(monday);
        }
        return membership;
    }

    private Attendance entry(LocalDateTime dateTime) {
        return attendanceService.registerEntry(DOCUMENT, dateTime, receptionist);
    }

    @Test
    @DisplayName("Permite la entrada con membresía activa dentro del horario del plan")
    void allowsEntry() {
        membership(TestData.basicPlan(), monday.minusDays(7), true);

        Attendance attendance = entry(monday.atTime(10, 0));

        assertTrue(attendance.isAllowed());
        assertNull(attendance.getRejectionReason());
        assertEquals(1, attendanceService.listByDate(monday).size());
    }

    @Test
    @DisplayName("Básico fuera de su horario: se rechaza y se guarda el motivo")
    void rejectsOutsidePlanHours() {
        membership(TestData.basicPlan(), monday.minusDays(7), true);

        Attendance afternoon = entry(monday.atTime(16, 0));
        Attendance night = entry(monday.atTime(23, 0));

        assertFalse(afternoon.isAllowed());
        assertEquals("Fuera del horario del plan Básico", afternoon.getRejectionReason());
        assertEquals("Fuera del horario del gimnasio", night.getRejectionReason());
        assertEquals(2, attendanceService.listByClient(client.getId()).size());
    }

    @Test
    @DisplayName("Estándar puede entrar el sábado en la mañana")
    void standardEntersOnSaturday() {
        membership(TestData.standardPlan(), monday.minusDays(7), true);

        assertTrue(entry(monday.plusDays(5).atTime(10, 0)).isAllowed());
    }

    @Test
    @DisplayName("Membresía vencida: el motivo dice la fecha de vencimiento")
    void rejectsExpiredMembership() {
        Membership expired = membership(TestData.standardPlan(), monday.minusMonths(2), true);

        Attendance attendance = entry(monday.atTime(10, 0));

        assertFalse(attendance.isAllowed());
        assertTrue(attendance.getRejectionReason().startsWith("Membresía vencida el "));
        assertTrue(attendance.getRejectionReason().endsWith(String.valueOf(expired.getEndDate().getYear())));
    }

    @Test
    @DisplayName("Renovación que aún no empieza: el motivo dice cuándo empieza")
    void rejectsScheduledMembership() {
        membership(TestData.standardPlan(), monday.plusDays(3), true);

        assertTrue(entry(monday.atTime(10, 0)).getRejectionReason().startsWith("La membresía empieza el "));
    }

    @Test
    @DisplayName("Membresía sin pagar o cliente sin membresía no pueden entrar")
    void rejectsWithoutPaidMembership() {
        assertEquals("El cliente no tiene una membresía activa", entry(monday.atTime(10, 0)).getRejectionReason());

        membership(TestData.standardPlan(), monday, false);

        assertEquals("La membresía está pendiente de pago", entry(monday.atTime(10, 0)).getRejectionReason());
    }

    @Test
    @DisplayName("Cliente inactivo se rechaza aunque tenga membresía")
    void rejectsInactiveClient() {
        membership(TestData.standardPlan(), monday.minusDays(7), true);
        client.setActive(false);
        clientDAO.update(client);

        assertEquals("El cliente está inactivo", entry(monday.atTime(10, 0)).getRejectionReason());
    }

    @Test
    @DisplayName("Historial por periodo: del más reciente al más antiguo y con fechas válidas")
    void listsByDateRange() {
        membership(TestData.standardPlan(), monday.minusDays(7), true);
        entry(monday.atTime(7, 0));
        entry(monday.plusDays(1).atTime(9, 0));
        entry(monday.plusDays(10).atTime(9, 0));

        List<Attendance> records = attendanceService.listByDateRange(monday, monday.plusDays(1));

        assertEquals(2, records.size());
        assertEquals(monday.plusDays(1).atTime(9, 0), records.get(0).getDateTime());
        assertThrows(BusinessRuleException.class, () -> attendanceService.listByDateRange(monday, null));
        assertThrows(BusinessRuleException.class,
                () -> attendanceService.listByDateRange(monday.plusDays(1), monday));
    }

    @Test
    @DisplayName("Documento inexistente o datos incompletos lanzan excepción")
    void validatesInput() {
        assertThrows(NotFoundException.class,
                () -> attendanceService.registerEntry("999999", monday.atTime(10, 0), receptionist));
        assertThrows(BusinessRuleException.class,
                () -> attendanceService.registerEntry(DOCUMENT, null, receptionist));
        assertThrows(BusinessRuleException.class,
                () -> attendanceService.registerEntry(DOCUMENT, monday.atTime(10, 0), null));
        assertEquals(0, attendanceDAO.count());
    }
}
