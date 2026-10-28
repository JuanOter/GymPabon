package com.gympabon.dao;

import com.gympabon.model.Attendance;
import java.time.LocalDate;
import java.util.List;

/**
 * Data access for entry records, allowed and rejected.
 */
public interface AttendanceDAO extends GenericDAO<Attendance, Integer> {

    /**
     * Returns the entry attempts of one day.
     *
     * @param date day to look for
     * @return the records of that day, newest first
     */
    List<Attendance> findByDate(LocalDate date);

    /**
     * Returns the entry history of a client.
     *
     * @param clientId identifier of the client
     * @return the client's records, newest first
     */
    List<Attendance> findByClient(int clientId);

    /**
     * Returns the entry attempts between two dates, both included.
     *
     * @param from first day of the period
     * @param to   last day of the period
     * @return the records of the period, oldest first
     */
    List<Attendance> findByDateRange(LocalDate from, LocalDate to);
}
