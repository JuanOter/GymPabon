package com.gympabon.model;

import com.gympabon.exception.BusinessRuleException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gym trainer. It controls its own quota so it never has more than
 * {@value #MAX_PREMIUM_CLIENTS} Premium clients at the same time.
 */
public class Trainer extends Person {

    public static final int MAX_PREMIUM_CLIENTS = 20;

    private String specialty;
    private String workSchedule;
    private boolean active = true;
    private final List<Membership> premiumMemberships = new ArrayList<>();

    public Trainer() {
    }

    public Trainer(String documentNumber, String firstName, String lastName, String phone, String email,
                   String specialty, String workSchedule) {
        super(documentNumber, firstName, lastName, phone, email);
        this.specialty = specialty;
        this.workSchedule = workSchedule;
    }

    /**
     * Returns how many Premium clients the trainer can still receive.
     *
     * @return the maximum quota minus the current Premium memberships
     */
    public int getAvailableSlots() {
        return MAX_PREMIUM_CLIENTS - premiumMemberships.size();
    }

    /**
     * Tells whether the trainer can receive another Premium client.
     *
     * @return true if there is at least one free slot
     */
    public boolean hasAvailableSlot() {
        return getAvailableSlots() > 0;
    }

    /**
     * Assigns a Premium membership to this trainer.
     *
     * @param membership Premium membership to assign
     * @throws BusinessRuleException if the trainer is inactive or has no free slots
     */
    public void assignMembership(Membership membership) {
        if (!active) {
            throw new BusinessRuleException("El entrenador " + getFullName() + " está inactivo");
        }
        if (!hasAvailableSlot()) {
            throw new BusinessRuleException("El entrenador " + getFullName() + " no tiene cupos disponibles");
        }
        premiumMemberships.add(membership);
    }

    /**
     * Frees the slot used by a membership that expired or was cancelled.
     *
     * @param membership membership to release
     */
    public void releaseMembership(Membership membership) {
        premiumMemberships.remove(membership);
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getWorkSchedule() {
        return workSchedule;
    }

    public void setWorkSchedule(String workSchedule) {
        this.workSchedule = workSchedule;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Membership> getPremiumMemberships() {
        return premiumMemberships;
    }
}
