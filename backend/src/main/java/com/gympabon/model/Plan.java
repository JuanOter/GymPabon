package com.gympabon.model;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Common data and rules of every plan. Each subclass decides its own access,
 * enrollment fee and trainer rules, so the rest of the system can treat any
 * plan in the same way.
 */
public abstract class Plan {

    public static final double ENROLLMENT_FEE = 20000;

    private int id;
    private String name;
    private double monthlyPrice;
    private String description;

    protected Plan() {
    }

    protected Plan(int id, String name, double monthlyPrice, String description) {
        this.id = id;
        this.name = name;
        this.monthlyPrice = monthlyPrice;
        this.description = description;
    }

    /**
     * Tells whether this plan allows a client to enter at the given moment.
     *
     * @param dateTime day and time of the entry attempt
     * @return true if the entry is allowed
     */
    public abstract boolean canAccess(LocalDateTime dateTime);

    /**
     * Tells whether the moment is inside the gym opening hours: Monday to
     * Friday from 5:00 to 22:00, Saturday from 6:00 to 18:00 and Sunday from
     * 7:00 to 13:00.
     *
     * @param dateTime moment to check
     * @return true if the gym is open
     */
    public boolean isWithinGymHours(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        DayOfWeek day = dateTime.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY) {
            return isBetween(time, LocalTime.of(6, 0), LocalTime.of(18, 0));
        }
        if (day == DayOfWeek.SUNDAY) {
            return isBetween(time, LocalTime.of(7, 0), LocalTime.of(13, 0));
        }
        return isBetween(time, LocalTime.of(5, 0), LocalTime.of(22, 0));
    }

    /**
     * Calculates the price of the months bought, applying the discount of the
     * duration: monthly price x months x (1 - discount).
     *
     * @param duration duration bought
     * @return the price in pesos, rounded to whole pesos
     */
    public double calculatePrice(MembershipDuration duration) {
        double price = monthlyPrice * duration.getMonths() * (1 - duration.getDiscountRate());
        return Math.round(price);
    }

    /**
     * Returns the enrollment fee of the plan. By default it is the standard fee.
     *
     * @return the enrollment fee in pesos
     */
    public double getEnrollmentFee() {
        return ENROLLMENT_FEE;
    }

    /**
     * Tells whether the plan includes a personal trainer. By default it does not.
     *
     * @return false unless a subclass says otherwise
     */
    public boolean includesTrainer() {
        return false;
    }

    protected static boolean isBetween(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && time.isBefore(end);
    }

    @Override
    public String toString() {
        return name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(double monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
