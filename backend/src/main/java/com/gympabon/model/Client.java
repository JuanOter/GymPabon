package com.gympabon.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Person registered in the gym. It knows its own membership and attendance
 * history, so it is the class that can tell whether it has an active
 * membership.
 */
public class Client extends Person {

    private LocalDate birthDate;
    private LocalDate registrationDate;
    private boolean active = true;
    private final List<Membership> memberships = new ArrayList<>();
    private final List<Attendance> attendances = new ArrayList<>();

    public Client() {
    }

    public Client(String documentNumber, String firstName, String lastName, String phone, String email,
                  LocalDate birthDate, LocalDate registrationDate) {
        super(documentNumber, firstName, lastName, phone, email);
        this.birthDate = birthDate;
        this.registrationDate = registrationDate;
    }

    /**
     * Adds a membership to the client's history.
     *
     * @param membership membership bought by this client
     */
    public void addMembership(Membership membership) {
        memberships.add(membership);
    }

    /**
     * Returns the membership that is active on the given date.
     *
     * @param date date to check
     * @return the active membership, or null if the client has none
     */
    public Membership getActiveMembership(LocalDate date) {
        for (Membership membership : memberships) {
            if (membership.isActiveOn(date)) {
                return membership;
            }
        }
        return null;
    }

    /**
     * Tells whether the client already had a paid membership. It is used to
     * know if the enrollment fee must be charged.
     *
     * @return true if at least one membership has a valid payment
     */
    public boolean hasPreviousMemberships() {
        for (Membership membership : memberships) {
            if (membership.isPaid()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds an entry attempt, allowed or rejected, to the client's history.
     *
     * @param attendance entry record
     */
    public void addAttendance(Attendance attendance) {
        attendances.add(attendance);
    }

    /** Marks the client as inactive without deleting the history. */
    public void deactivate() {
        this.active = false;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Membership> getMemberships() {
        return memberships;
    }

    public List<Attendance> getAttendances() {
        return attendances;
    }
}
