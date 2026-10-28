package com.gympabon.dao.impl;

import com.gympabon.dao.PaymentDAO;
import com.gympabon.exception.DataAccessException;
import com.gympabon.model.Client;
import com.gympabon.model.Membership;
import com.gympabon.model.MembershipStatus;
import com.gympabon.model.Payment;
import com.gympabon.model.PaymentMethod;
import com.gympabon.model.User;
import com.gympabon.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of {@link PaymentDAO}. Each payment is loaded with its
 * membership (client and plan) and the user who registered it.
 */
@Repository
public class PaymentDAOImpl implements PaymentDAO {

    private static final String SELECT_PAYMENT = """
            SELECT pay.id, pay.amount, pay.method, pay.payment_date, pay.voided, pay.void_reason,
                   m.id AS membership_id, m.start_date, m.end_date, m.total_amount, m.status,
                   c.id AS client_id, c.document_number AS client_document,
                   c.first_name AS client_first_name, c.last_name AS client_last_name,
                   p.id AS plan_id, p.plan_type, p.name AS plan_name, p.monthly_price, p.description,
                   u.id AS user_id, u.username, u.first_name AS user_first_name, u.last_name AS user_last_name
            FROM payment pay
            JOIN membership m ON m.id = pay.membership_id
            JOIN client c ON c.id = m.client_id
            JOIN plan p ON p.id = m.plan_id
            JOIN app_user u ON u.id = pay.registered_by
            """;

    private final DatabaseConnection databaseConnection;

    public PaymentDAOImpl(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public void save(Payment payment) {
        String sql = """
                INSERT INTO payment (membership_id, amount, method, payment_date, registered_by, voided, void_reason)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillStatement(statement, payment);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    payment.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible guardar el pago", e);
        }
    }

    @Override
    public void update(Payment payment) {
        String sql = """
                UPDATE payment SET membership_id = ?, amount = ?, method = ?, payment_date = ?, registered_by = ?,
                       voided = ?, void_reason = ?
                WHERE id = ?
                """;
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, payment);
            statement.setInt(8, payment.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible actualizar el pago", e);
        }
    }

    @Override
    public Payment findById(Integer id) {
        List<Payment> result = query(SELECT_PAYMENT + " WHERE pay.id = ?", id);
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<Payment> findAll() {
        return query(SELECT_PAYMENT + " ORDER BY pay.payment_date DESC, pay.id DESC");
    }

    @Override
    public void delete(Integer id) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM payment WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible eliminar el pago", e);
        }
    }

    @Override
    public List<Payment> findByMembership(int membershipId) {
        return query(SELECT_PAYMENT + " WHERE pay.membership_id = ? ORDER BY pay.payment_date DESC, pay.id DESC",
                membershipId);
    }

    @Override
    public List<Payment> findByDateRange(LocalDate from, LocalDate to, PaymentMethod method) {
        String order = " ORDER BY pay.payment_date DESC, pay.id DESC";
        if (method == null) {
            return query(SELECT_PAYMENT + " WHERE pay.payment_date BETWEEN ? AND ?" + order, from, to);
        }
        return query(SELECT_PAYMENT + " WHERE pay.payment_date BETWEEN ? AND ? AND pay.method = ?" + order,
                from, to, method.name());
    }

    private List<Payment> query(String sql, Object... parameters) {
        List<Payment> payments = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    payments.add(mapPayment(resultSet));
                }
            }
            return payments;
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible consultar los pagos", e);
        }
    }

    private void fillStatement(PreparedStatement statement, Payment payment) throws SQLException {
        statement.setInt(1, payment.getMembership().getId());
        statement.setDouble(2, payment.getAmount());
        statement.setString(3, payment.getMethod().name());
        statement.setObject(4, payment.getPaymentDate());
        statement.setInt(5, payment.getRegisteredBy().getId());
        statement.setBoolean(6, payment.isVoided());
        statement.setString(7, payment.getVoidReason());
    }

    private Payment mapPayment(ResultSet resultSet) throws SQLException {
        Payment payment = new Payment();
        payment.setId(resultSet.getInt("id"));
        payment.setAmount(resultSet.getDouble("amount"));
        payment.setMethod(PaymentMethod.valueOf(resultSet.getString("method")));
        payment.setPaymentDate(resultSet.getObject("payment_date", LocalDate.class));
        payment.setVoided(resultSet.getBoolean("voided"));
        payment.setVoidReason(resultSet.getString("void_reason"));
        payment.setMembership(mapMembership(resultSet));
        payment.setRegisteredBy(mapUser(resultSet));
        return payment;
    }

    private Membership mapMembership(ResultSet resultSet) throws SQLException {
        Client client = new Client();
        client.setId(resultSet.getInt("client_id"));
        client.setDocumentNumber(resultSet.getString("client_document"));
        client.setFirstName(resultSet.getString("client_first_name"));
        client.setLastName(resultSet.getString("client_last_name"));

        Membership membership = new Membership();
        membership.setId(resultSet.getInt("membership_id"));
        membership.setStartDate(resultSet.getObject("start_date", LocalDate.class));
        membership.setEndDate(resultSet.getObject("end_date", LocalDate.class));
        membership.setTotalAmount(resultSet.getDouble("total_amount"));
        membership.setStatus(MembershipStatus.valueOf(resultSet.getString("status")));
        membership.setClient(client);
        membership.setPlan(PlanDAOImpl.createPlan(resultSet.getString("plan_type"), resultSet.getInt("plan_id"),
                resultSet.getString("plan_name"), resultSet.getDouble("monthly_price"),
                resultSet.getString("description")));
        return membership;
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getInt("user_id"));
        user.setUsername(resultSet.getString("username"));
        user.setFirstName(resultSet.getString("user_first_name"));
        user.setLastName(resultSet.getString("user_last_name"));
        return user;
    }
}
