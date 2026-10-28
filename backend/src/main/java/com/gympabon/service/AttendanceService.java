package com.gympabon.service;

import com.gympabon.dao.AttendanceDAO;
import com.gympabon.dao.ClientDAO;
import com.gympabon.dao.MembershipDAO;
import com.gympabon.exception.BusinessRuleException;
import com.gympabon.exception.NotFoundException;
import com.gympabon.model.Attendance;
import com.gympabon.model.Client;
import com.gympabon.model.Membership;
import com.gympabon.model.MembershipStatus;
import com.gympabon.model.Plan;
import com.gympabon.model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Access control at the gym entrance. Every attempt is saved, allowed or
 * rejected, with the reason of the rejection.
 */
@Service
public class AttendanceService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AttendanceDAO attendanceDAO;
    private final ClientDAO clientDAO;
    private final MembershipDAO membershipDAO;

    public AttendanceService(AttendanceDAO attendanceDAO, ClientDAO clientDAO, MembershipDAO membershipDAO) {
        this.attendanceDAO = attendanceDAO;
        this.clientDAO = clientDAO;
        this.membershipDAO = membershipDAO;
    }

    /**
     * Checks whether a client can enter and saves the attempt. The client
     * must be active, have an active membership and its plan must allow
     * entry at that moment.
     *
     * @param documentNumber document of the client
     * @param dateTime       moment of the attempt
     * @param registeredBy   user at the reception
     * @return the saved record, allowed or rejected with its reason
     * @throws NotFoundException     if no client has that document
     * @throws BusinessRuleException if the moment or the user are missing
     */
    public Attendance registerEntry(String documentNumber, LocalDateTime dateTime, User registeredBy) {
        if (dateTime == null || registeredBy == null) {
            throw new BusinessRuleException("La fecha, la hora y el usuario son obligatorios");
        }
        Client client = clientDAO.findByDocument(documentNumber == null ? "" : documentNumber.trim());
        if (client == null) {
            throw new NotFoundException("No existe un cliente con el documento " + documentNumber);
        }
        client.getMemberships().addAll(membershipDAO.findByClient(client.getId()));

        String rejectionReason = findRejectionReason(client, dateTime);
        Attendance attendance = rejectionReason == null
                ? Attendance.createAllowed(client, dateTime, registeredBy)
                : Attendance.createRejected(client, dateTime, registeredBy, rejectionReason);
        attendanceDAO.save(attendance);
        client.addAttendance(attendance);
        return attendance;
    }

    /**
     * Returns the entry attempts of a day.
     *
     * @param date day to look for
     * @return the records of that day, newest first
     */
    public List<Attendance> listByDate(LocalDate date) {
        return attendanceDAO.findByDate(date == null ? LocalDate.now() : date);
    }

    /**
     * Returns the entry history of a client.
     *
     * @param clientId identifier of the client
     * @return the client's records, newest first
     */
    public List<Attendance> listByClient(int clientId) {
        return attendanceDAO.findByClient(clientId);
    }

    private String findRejectionReason(Client client, LocalDateTime dateTime) {
        if (!client.isActive()) {
            return "El cliente está inactivo";
        }
        LocalDate date = dateTime.toLocalDate();
        Membership active = client.getActiveMembership(date);
        if (active == null) {
            return explainMissingMembership(client.getMemberships(), date);
        }
        Plan plan = active.getPlan();
        if (!plan.canAccess(dateTime)) {
            return plan.isWithinGymHours(dateTime)
                    ? "Fuera del horario del plan " + plan.getName()
                    : "Fuera del horario del gimnasio";
        }
        return null;
    }

    private String explainMissingMembership(List<Membership> memberships, LocalDate date) {
        Optional<Membership> upcoming = memberships.stream()
                .filter(m -> m.isPaid() && m.getStatus() != MembershipStatus.CANCELLED
                        && m.getStartDate().isAfter(date))
                .min(Comparator.comparing(Membership::getStartDate));
        if (upcoming.isPresent()) {
            return "La membresía empieza el " + upcoming.get().getStartDate().format(DATE_FORMAT);
        }
        Optional<Membership> lastExpired = memberships.stream()
                .filter(m -> m.isPaid() && m.getStatus() != MembershipStatus.CANCELLED
                        && m.getEndDate().isBefore(date))
                .max(Comparator.comparing(Membership::getEndDate));
        if (lastExpired.isPresent()) {
            return "Membresía vencida el " + lastExpired.get().getEndDate().format(DATE_FORMAT);
        }
        boolean pending = memberships.stream().anyMatch(m -> m.getStatus() == MembershipStatus.PENDING_PAYMENT);
        return pending ? "La membresía está pendiente de pago" : "El cliente no tiene una membresía activa";
    }
}
