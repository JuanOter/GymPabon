package com.gympabon.fake;

import com.gympabon.dao.AttendanceDAO;
import com.gympabon.model.Attendance;
import java.time.LocalDate;
import java.util.List;

/** In-memory {@link AttendanceDAO} for unit tests. */
public class InMemoryAttendanceDAO extends InMemoryDAO<Attendance> implements AttendanceDAO {

    @Override
    protected int idOf(Attendance attendance) {
        return attendance.getId();
    }

    @Override
    protected void assignId(Attendance attendance, int id) {
        attendance.setId(id);
    }

    @Override
    public List<Attendance> findByDate(LocalDate date) {
        return findByDateRange(date, date);
    }

    @Override
    public List<Attendance> findByClient(int clientId) {
        return rows.values().stream().filter(a -> a.getClient().getId() == clientId).toList();
    }

    @Override
    public List<Attendance> findByDateRange(LocalDate from, LocalDate to) {
        return rows.values().stream().filter(a -> {
            LocalDate day = a.getDateTime().toLocalDate();
            return !day.isBefore(from) && !day.isAfter(to);
        }).toList();
    }
}
