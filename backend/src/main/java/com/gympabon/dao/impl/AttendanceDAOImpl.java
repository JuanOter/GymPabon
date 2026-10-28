package com.gympabon.dao.impl;

import com.gympabon.dao.AttendanceDAO;
import com.gympabon.exception.DataAccessException;
import com.gympabon.model.Attendance;
import com.gympabon.model.Client;
import com.gympabon.model.User;
import com.gympabon.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of {@link AttendanceDAO}. Each record is loaded with the
 * client and the user who registered it.
 */
@Repository
public class AttendanceDAOImpl implements AttendanceDAO {

    private static final String SELECT_ATTENDANCE = """
            SELECT a.id, a.date_time, a.allowed, a.rejection_reason,
                   c.id AS client_id, c.document_number AS client_document,
                   c.first_name AS client_first_name, c.last_name AS client_last_name,
                   u.id AS user_id, u.username, u.first_name AS user_first_name, u.last_name AS user_last_name
            FROM attendance a
            JOIN client c ON c.id = a.client_id
            JOIN app_user u ON u.id = a.registered_by
            """;

    private final DatabaseConnection databaseConnection;

    public AttendanceDAOImpl(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public void save(Attendance attendance) {
        String sql = """
                INSERT INTO attendance (client_id, date_time, allowed, rejection_reason, registered_by)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillStatement(statement, attendance);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    attendance.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible guardar la entrada", e);
        }
    }

    @Override
    public void update(Attendance attendance) {
        String sql = """
                UPDATE attendance SET client_id = ?, date_time = ?, allowed = ?, rejection_reason = ?,
                       registered_by = ?
                WHERE id = ?
                """;
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, attendance);
            statement.setInt(6, attendance.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible actualizar la entrada", e);
        }
    }

    @Override
    public Attendance findById(Integer id) {
        List<Attendance> result = query(SELECT_ATTENDANCE + " WHERE a.id = ?", id);
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<Attendance> findAll() {
        return query(SELECT_ATTENDANCE + " ORDER BY a.date_time DESC");
    }

    @Override
    public void delete(Integer id) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM attendance WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible eliminar la entrada", e);
        }
    }

    @Override
    public List<Attendance> findByDate(LocalDate date) {
        return query(SELECT_ATTENDANCE + " WHERE a.date_time >= ? AND a.date_time < ? ORDER BY a.date_time DESC",
                date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    @Override
    public List<Attendance> findByClient(int clientId) {
        return query(SELECT_ATTENDANCE + " WHERE a.client_id = ? ORDER BY a.date_time DESC", clientId);
    }

    @Override
    public List<Attendance> findByDateRange(LocalDate from, LocalDate to) {
        return query(SELECT_ATTENDANCE + " WHERE a.date_time >= ? AND a.date_time < ? ORDER BY a.date_time",
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    private List<Attendance> query(String sql, Object... parameters) {
        List<Attendance> records = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(mapAttendance(resultSet));
                }
            }
            return records;
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible consultar las entradas", e);
        }
    }

    private void fillStatement(PreparedStatement statement, Attendance attendance) throws SQLException {
        statement.setInt(1, attendance.getClient().getId());
        statement.setObject(2, attendance.getDateTime());
        statement.setBoolean(3, attendance.isAllowed());
        statement.setString(4, attendance.getRejectionReason());
        statement.setInt(5, attendance.getRegisteredBy().getId());
    }

    private Attendance mapAttendance(ResultSet resultSet) throws SQLException {
        Client client = new Client();
        client.setId(resultSet.getInt("client_id"));
        client.setDocumentNumber(resultSet.getString("client_document"));
        client.setFirstName(resultSet.getString("client_first_name"));
        client.setLastName(resultSet.getString("client_last_name"));

        User user = new User();
        user.setId(resultSet.getInt("user_id"));
        user.setUsername(resultSet.getString("username"));
        user.setFirstName(resultSet.getString("user_first_name"));
        user.setLastName(resultSet.getString("user_last_name"));

        Attendance attendance = new Attendance();
        attendance.setId(resultSet.getInt("id"));
        attendance.setDateTime(resultSet.getObject("date_time", LocalDateTime.class));
        attendance.setAllowed(resultSet.getBoolean("allowed"));
        attendance.setRejectionReason(resultSet.getString("rejection_reason"));
        attendance.setClient(client);
        attendance.setRegisteredBy(user);
        return attendance;
    }
}
