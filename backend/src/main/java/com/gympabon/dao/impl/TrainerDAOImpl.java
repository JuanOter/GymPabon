package com.gympabon.dao.impl;

import com.gympabon.dao.TrainerDAO;
import com.gympabon.exception.DataAccessException;
import com.gympabon.model.Membership;
import com.gympabon.model.MembershipStatus;
import com.gympabon.model.Trainer;
import com.gympabon.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of {@link TrainerDAO}. Every trainer is loaded with the
 * Premium memberships that still use one of its slots: pending payment,
 * scheduled or active.
 */
@Repository
public class TrainerDAOImpl implements TrainerDAO {

    private static final String SELECT_TRAINER = """
            SELECT id, document_number, first_name, last_name, phone, email, specialty, work_schedule, active
            FROM trainer
            """;

    private static final String SELECT_CURRENT_MEMBERSHIPS = """
            SELECT id, trainer_id, status FROM membership
            WHERE trainer_id IS NOT NULL AND status IN ('PENDING_PAYMENT', 'SCHEDULED', 'ACTIVE')
            """;

    private final DatabaseConnection databaseConnection;

    public TrainerDAOImpl(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public void save(Trainer trainer) {
        String sql = """
                INSERT INTO trainer (document_number, first_name, last_name, phone, email, specialty,
                                     work_schedule, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillStatement(statement, trainer);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    trainer.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible guardar el entrenador", e);
        }
    }

    @Override
    public void update(Trainer trainer) {
        String sql = """
                UPDATE trainer SET document_number = ?, first_name = ?, last_name = ?, phone = ?, email = ?,
                       specialty = ?, work_schedule = ?, active = ?
                WHERE id = ?
                """;
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, trainer);
            statement.setInt(9, trainer.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible actualizar el entrenador", e);
        }
    }

    @Override
    public Trainer findById(Integer id) {
        List<Trainer> result = query(SELECT_TRAINER + " WHERE id = ?", id);
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<Trainer> findAll() {
        return query(SELECT_TRAINER + " ORDER BY first_name, last_name");
    }

    @Override
    public void delete(Integer id) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM trainer WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible eliminar el entrenador; puede tener membresías asociadas", e);
        }
    }

    @Override
    public Trainer findByDocument(String documentNumber) {
        List<Trainer> result = query(SELECT_TRAINER + " WHERE document_number = ?", documentNumber);
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<Trainer> findActive() {
        return query(SELECT_TRAINER + " WHERE active = TRUE ORDER BY first_name, last_name");
    }

    private List<Trainer> query(String sql, Object... parameters) {
        Map<Integer, Trainer> trainers = new LinkedHashMap<>();
        try (Connection connection = databaseConnection.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    statement.setObject(i + 1, parameters[i]);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Trainer trainer = mapTrainer(resultSet);
                        trainers.put(trainer.getId(), trainer);
                    }
                }
            }
            if (!trainers.isEmpty()) {
                loadCurrentMemberships(connection, trainers);
            }
            return new ArrayList<>(trainers.values());
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible consultar los entrenadores", e);
        }
    }

    private void loadCurrentMemberships(Connection connection, Map<Integer, Trainer> trainers) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_CURRENT_MEMBERSHIPS);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Trainer trainer = trainers.get(resultSet.getInt("trainer_id"));
                if (trainer != null) {
                    Membership membership = new Membership();
                    membership.setId(resultSet.getInt("id"));
                    membership.setStatus(MembershipStatus.valueOf(resultSet.getString("status")));
                    membership.setTrainer(trainer);
                    trainer.getPremiumMemberships().add(membership);
                }
            }
        }
    }

    private void fillStatement(PreparedStatement statement, Trainer trainer) throws SQLException {
        statement.setString(1, trainer.getDocumentNumber());
        statement.setString(2, trainer.getFirstName());
        statement.setString(3, trainer.getLastName());
        statement.setString(4, trainer.getPhone());
        statement.setString(5, trainer.getEmail());
        statement.setString(6, trainer.getSpecialty());
        statement.setString(7, trainer.getWorkSchedule());
        statement.setBoolean(8, trainer.isActive());
    }

    private Trainer mapTrainer(ResultSet resultSet) throws SQLException {
        Trainer trainer = new Trainer();
        trainer.setId(resultSet.getInt("id"));
        trainer.setDocumentNumber(resultSet.getString("document_number"));
        trainer.setFirstName(resultSet.getString("first_name"));
        trainer.setLastName(resultSet.getString("last_name"));
        trainer.setPhone(resultSet.getString("phone"));
        trainer.setEmail(resultSet.getString("email"));
        trainer.setSpecialty(resultSet.getString("specialty"));
        trainer.setWorkSchedule(resultSet.getString("work_schedule"));
        trainer.setActive(resultSet.getBoolean("active"));
        return trainer;
    }
}
