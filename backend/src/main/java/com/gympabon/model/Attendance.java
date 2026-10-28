package com.gympabon.model;

import com.gympabon.exception.BusinessRuleException;
import java.time.LocalDateTime;

/**
 * Record of every entry attempt of a client, allowed or rejected, used for
 * access control and reports.
 */
public class Attendance {

    private int id;
    private Client client;
    private LocalDateTime dateTime;
    private boolean allowed;
    private String rejectionReason;
    private User registeredBy;

    /** Empty constructor used when a record is loaded from the database. */
    public Attendance() {
    }

    private Attendance(Client client, LocalDateTime dateTime, boolean allowed, String rejectionReason,
                       User registeredBy) {
        this.client = client;
        this.dateTime = dateTime;
        this.allowed = allowed;
        this.rejectionReason = rejectionReason;
        this.registeredBy = registeredBy;
    }

    /**
     * Creates the record of an allowed entry.
     *
     * @param client   client who entered
     * @param dateTime moment of the entry
     * @param user     user who registered it
     * @return the new record
     */
    public static Attendance createAllowed(Client client, LocalDateTime dateTime, User user) {
        return new Attendance(client, dateTime, true, null, user);
    }

    /**
     * Creates the record of a rejected entry with its reason.
     *
     * @param client   client who tried to enter
     * @param dateTime moment of the attempt
     * @param user     user who registered it
     * @param reason   why the entry was rejected
     * @return the new record
     * @throws BusinessRuleException if the reason is empty
     */
    public static Attendance createRejected(Client client, LocalDateTime dateTime, User user, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("El motivo del rechazo es obligatorio");
        }
        return new Attendance(client, dateTime, false, reason.trim(), user);
    }

    public boolean isAllowed() {
        return allowed;
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

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public User getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(User registeredBy) {
        this.registeredBy = registeredBy;
    }
}
