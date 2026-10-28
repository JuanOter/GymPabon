package com.gympabon.dao.impl;

import com.gympabon.dao.MembershipDAO;
import com.gympabon.exception.DataAccessException;
import com.gympabon.model.Client;
import com.gympabon.model.Membership;
import com.gympabon.model.MembershipDuration;
import com.gympabon.model.MembershipStatus;
import com.gympabon.model.Payment;
import com.gympabon.model.PaymentMethod;
import com.gympabon.model.Trainer;
import com.gympabon.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of {@link MembershipDAO}. Each membership is loaded with
 * its client, plan, trainer and valid payment in a single query.
 */
@Repository
public class MembershipDAOImpl implements MembershipDAO {

    private static final String SELECT_MEMBERSHIP = """
            SELECT m.id, m.duration, m.start_date, m.end_date, m.enrollment_fee, m.total_amount,
                   m.status, m.cancellation_reason,
                   c.id AS client_id, c.document_number AS client_document,
                   c.first_name AS client_first_name, c.last_name AS client_last_name,
                   p.id AS plan_id, p.plan_type, p.name AS plan_name, p.monthly_price, p.description,
                   t.id AS trainer_id, t.first_name AS trainer_first_name, t.last_name AS trainer_last_name,
                   pay.id AS payment_id, pay.amount AS payment_amount, pay.method AS payment_method,
                   pay.payment_date
            FROM membership m
            JOIN client c ON c.id = m.client_id
            JOIN plan p ON p.id = m.plan_id
            LEFT JOIN trainer t ON t.id = m.trainer_id
            LEFT JOIN payment pay ON pay.id = (SELECT MAX(id) FROM payment
                                               WHERE membership_id = m.id AND voided = FALSE)
            """;

    private final DatabaseConnection databaseConnection;

    public MembershipDAOImpl(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public void save(Membership membership) {
        String sql = """
                INSERT INTO membership (client_id, plan_id, trainer_id, duration, start_date, end_date,
                                        enrollment_fee, total_amount, status, cancellation_reason)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillStatement(statement, membership);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    membership.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible guardar la membresía", e);
        }
    }

    @Override
    public void update(Membership membership) {
        String sql = """
                UPDATE membership SET client_id = ?, plan_id = ?, trainer_id = ?, duration = ?, start_date = ?,
                       end_date = ?, enrollment_fee = ?, total_amount = ?, status = ?, cancellation_reason = ?
                WHERE id = ?
                """;
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, membership);
            statement.setInt(11, membership.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible actualizar la membresía", e);
        }
    }

    @Override
    public Membership findById(Integer id) {
        List<Membership> result = query(SELECT_MEMBERSHIP + " WHERE m.id = ?", id);
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<Membership> findAll() {
        return query(SELECT_MEMBERSHIP + " ORDER BY m.start_date DESC, m.id DESC");
    }

    @Override
    public void delete(Integer id) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM membership WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible eliminar la membresía; puede tener pagos asociados", e);
        }
    }

    @Override
    public List<Membership> findByClient(int clientId) {
        return query(SELECT_MEMBERSHIP + " WHERE m.client_id = ? ORDER BY m.start_date DESC, m.id DESC", clientId);
    }

    @Override
    public List<Membership> findByStatus(MembershipStatus status) {
        return query(SELECT_MEMBERSHIP + " WHERE m.status = ? ORDER BY m.start_date, m.id", status.name());
    }

    private List<Membership> query(String sql, Object... parameters) {
        List<Membership> memberships = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    memberships.add(mapMembership(resultSet));
                }
            }
            return memberships;
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible consultar las membresías", e);
        }
    }

    private void fillStatement(PreparedStatement statement, Membership membership) throws SQLException {
        statement.setInt(1, membership.getClient().getId());
        statement.setInt(2, membership.getPlan().getId());
        if (membership.getTrainer() == null) {
            statement.setNull(3, Types.INTEGER);
        } else {
            statement.setInt(3, membership.getTrainer().getId());
        }
        statement.setString(4, membership.getDuration().name());
        statement.setObject(5, membership.getStartDate());
        statement.setObject(6, membership.getEndDate());
        statement.setDouble(7, membership.getEnrollmentFee());
        statement.setDouble(8, membership.getTotalAmount());
        statement.setString(9, membership.getStatus().name());
        statement.setString(10, membership.getCancellationReason());
    }

    private Membership mapMembership(ResultSet resultSet) throws SQLException {
        Membership membership = new Membership();
        membership.setId(resultSet.getInt("id"));
        membership.setDuration(MembershipDuration.valueOf(resultSet.getString("duration")));
        membership.setStartDate(resultSet.getObject("start_date", LocalDate.class));
        membership.setEndDate(resultSet.getObject("end_date", LocalDate.class));
        membership.setEnrollmentFee(resultSet.getDouble("enrollment_fee"));
        membership.setTotalAmount(resultSet.getDouble("total_amount"));
        membership.setStatus(MembershipStatus.valueOf(resultSet.getString("status")));
        membership.setCancellationReason(resultSet.getString("cancellation_reason"));
        membership.setClient(mapClient(resultSet));
        membership.setPlan(PlanDAOImpl.createPlan(resultSet.getString("plan_type"), resultSet.getInt("plan_id"),
                resultSet.getString("plan_name"), resultSet.getDouble("monthly_price"),
                resultSet.getString("description")));
        membership.setTrainer(mapTrainer(resultSet));
        membership.setPayment(mapPayment(resultSet, membership));
        return membership;
    }

    private Client mapClient(ResultSet resultSet) throws SQLException {
        Client client = new Client();
        client.setId(resultSet.getInt("client_id"));
        client.setDocumentNumber(resultSet.getString("client_document"));
        client.setFirstName(resultSet.getString("client_first_name"));
        client.setLastName(resultSet.getString("client_last_name"));
        return client;
    }

    private Trainer mapTrainer(ResultSet resultSet) throws SQLException {
        Integer trainerId = resultSet.getObject("trainer_id", Integer.class);
        if (trainerId == null) {
            return null;
        }
        Trainer trainer = new Trainer();
        trainer.setId(trainerId);
        trainer.setFirstName(resultSet.getString("trainer_first_name"));
        trainer.setLastName(resultSet.getString("trainer_last_name"));
        return trainer;
    }

    private Payment mapPayment(ResultSet resultSet, Membership membership) throws SQLException {
        Integer paymentId = resultSet.getObject("payment_id", Integer.class);
        if (paymentId == null) {
            return null;
        }
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setMembership(membership);
        payment.setAmount(resultSet.getDouble("payment_amount"));
        payment.setMethod(PaymentMethod.valueOf(resultSet.getString("payment_method")));
        payment.setPaymentDate(resultSet.getObject("payment_date", LocalDate.class));
        return payment;
    }
}
