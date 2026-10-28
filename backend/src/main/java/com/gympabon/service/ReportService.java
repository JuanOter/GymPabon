package com.gympabon.service;

import com.gympabon.dao.AttendanceDAO;
import com.gympabon.dao.ClientDAO;
import com.gympabon.dao.MembershipDAO;
import com.gympabon.dao.PaymentDAO;
import com.gympabon.exception.BusinessRuleException;
import com.gympabon.model.Attendance;
import com.gympabon.model.Membership;
import com.gympabon.model.MembershipStatus;
import com.gympabon.model.Payment;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

/**
 * Figures for the main panel and the administrator reports.
 */
@Service
public class ReportService {

    private static final int EXPIRING_DAYS = 7;

    private final ClientDAO clientDAO;
    private final MembershipDAO membershipDAO;
    private final PaymentDAO paymentDAO;
    private final AttendanceDAO attendanceDAO;

    public ReportService(ClientDAO clientDAO, MembershipDAO membershipDAO, PaymentDAO paymentDAO,
                         AttendanceDAO attendanceDAO) {
        this.clientDAO = clientDAO;
        this.membershipDAO = membershipDAO;
        this.paymentDAO = paymentDAO;
        this.attendanceDAO = attendanceDAO;
    }

    /**
     * Returns the figures of the main panel: clients with an active
     * membership, income of the month, entries of the day, memberships that
     * expire in the next days and registered clients.
     *
     * @param today reference date
     * @return the figures by name
     */
    public Map<String, Object> getDashboardSummary(LocalDate today) {
        List<Membership> active = membershipDAO.findByStatus(MembershipStatus.ACTIVE);
        Set<Integer> activeClients = new HashSet<>();
        active.forEach(m -> activeClients.add(m.getClient().getId()));

        double monthlyIncome = sumValidPayments(paymentDAO.findByDateRange(today.withDayOfMonth(1), today, null));
        long todayEntries = attendanceDAO.findByDate(today).stream().filter(Attendance::isAllowed).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("activeClients", activeClients.size());
        summary.put("monthlyIncome", monthlyIncome);
        summary.put("todayEntries", todayEntries);
        summary.put("expiringMemberships", findExpiring(active, today, EXPIRING_DAYS).size());
        summary.put("registeredClients", clientDAO.findAll().size());
        return summary;
    }

    /**
     * Returns the money collected in a period by plan, without voided payments.
     *
     * @param from first day of the period
     * @param to   last day of the period
     * @return the income by plan name
     */
    public Map<String, Double> getIncomeByPlan(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        Map<String, Double> income = new LinkedHashMap<>();
        for (Payment payment : paymentDAO.findByDateRange(from, to, null)) {
            if (payment.isValid()) {
                income.merge(payment.getMembership().getPlan().getName(), payment.getAmount(), Double::sum);
            }
        }
        return income;
    }

    /**
     * Returns the active memberships that end within the next days.
     *
     * @param days number of days to look ahead
     * @return the memberships ordered by end date
     */
    public List<Membership> getExpiringMemberships(int days) {
        if (days < 0) {
            throw new BusinessRuleException("El número de días no puede ser negativo");
        }
        return findExpiring(membershipDAO.findByStatus(MembershipStatus.ACTIVE), LocalDate.now(), days);
    }

    /**
     * Returns how many clients have an active membership in each plan.
     *
     * @return the number of clients by plan name
     */
    public Map<String, Integer> getClientsByPlan() {
        Map<String, Integer> clientsByPlan = new LinkedHashMap<>();
        for (Membership membership : membershipDAO.findByStatus(MembershipStatus.ACTIVE)) {
            clientsByPlan.merge(membership.getPlan().getName(), 1, Integer::sum);
        }
        return clientsByPlan;
    }

    /**
     * Returns the number of allowed entries per day in a period.
     *
     * @param from first day of the period
     * @param to   last day of the period
     * @return the entries by day, in date order
     */
    public Map<LocalDate, Integer> getAttendanceByDay(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        Map<LocalDate, Integer> entries = new TreeMap<>();
        for (Attendance attendance : attendanceDAO.findByDateRange(from, to)) {
            if (attendance.isAllowed()) {
                entries.merge(attendance.getDateTime().toLocalDate(), 1, Integer::sum);
            }
        }
        return entries;
    }

    private List<Membership> findExpiring(List<Membership> active, LocalDate today, int days) {
        LocalDate limit = today.plusDays(days);
        return active.stream()
                .filter(m -> !m.getEndDate().isBefore(today) && !m.getEndDate().isAfter(limit))
                .sorted(Comparator.comparing(Membership::getEndDate))
                .toList();
    }

    private double sumValidPayments(List<Payment> payments) {
        return payments.stream().filter(Payment::isValid).mapToDouble(Payment::getAmount).sum();
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessRuleException("Debe indicar la fecha inicial y la final");
        }
        if (from.isAfter(to)) {
            throw new BusinessRuleException("La fecha inicial no puede ser posterior a la final");
        }
    }
}
