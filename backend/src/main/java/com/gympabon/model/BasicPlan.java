package com.gympabon.model;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Basic plan ($70.000 per month), the only one with restricted hours: Monday
 * to Friday from 5:00 to 15:00.
 */
public class BasicPlan extends Plan {

    public static final LocalTime ACCESS_START = LocalTime.of(5, 0);
    public static final LocalTime ACCESS_END = LocalTime.of(15, 0);

    public BasicPlan() {
    }

    public BasicPlan(int id, String name, double monthlyPrice, String description) {
        super(id, name, monthlyPrice, description);
    }

    @Override
    public boolean canAccess(LocalDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        boolean weekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
        return weekday && isBetween(dateTime.toLocalTime(), ACCESS_START, ACCESS_END);
    }
}
