package com.gympabon.dao.impl;

import com.gympabon.dao.ClientDAO;
import com.gympabon.exception.DataAccessException;
import com.gympabon.model.Client;
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
 * JDBC implementation of {@link ClientDAO}.
 */
@Repository
public class ClientDAOImpl implements ClientDAO {

    private static final String SELECT_CLIENT = """
            SELECT id, document_number, first_name, last_name, phone, email, birth_date,
                   registration_date, active
            FROM client
            """;

    private final DatabaseConnection databaseConnection;

    public ClientDAOImpl(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public void save(Client client) {
        String sql = """
                INSERT INTO client (document_number, first_name, last_name, phone, email, birth_date,
                                    registration_date, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillStatement(statement, client);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    client.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible guardar el cliente", e);
        }
    }

    @Override
    public void update(Client client) {
        String sql = """
                UPDATE client SET document_number = ?, first_name = ?, last_name = ?, phone = ?, email = ?,
                       birth_date = ?, registration_date = ?, active = ?
                WHERE id = ?
                """;
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, client);
            statement.setInt(9, client.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible actualizar el cliente", e);
        }
    }

    @Override
    public Client findById(Integer id) {
        List<Client> result = query(SELECT_CLIENT + " WHERE id = ?", id);
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<Client> findAll() {
        return query(SELECT_CLIENT + " ORDER BY first_name, last_name");
    }

    @Override
    public void delete(Integer id) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM client WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible eliminar el cliente; puede tener membresías asociadas", e);
        }
    }

    @Override
    public Client findByDocument(String documentNumber) {
        List<Client> result = query(SELECT_CLIENT + " WHERE document_number = ?", documentNumber);
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<Client> search(String text) {
        String pattern = "%" + text.trim() + "%";
        return query(SELECT_CLIENT
                + " WHERE document_number LIKE ? OR CONCAT(first_name, ' ', last_name) LIKE ?"
                + " ORDER BY first_name, last_name", pattern, pattern);
    }

    private List<Client> query(String sql, Object... parameters) {
        List<Client> clients = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    clients.add(mapClient(resultSet));
                }
            }
            return clients;
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible consultar los clientes", e);
        }
    }

    private void fillStatement(PreparedStatement statement, Client client) throws SQLException {
        statement.setString(1, client.getDocumentNumber());
        statement.setString(2, client.getFirstName());
        statement.setString(3, client.getLastName());
        statement.setString(4, client.getPhone());
        statement.setString(5, client.getEmail());
        statement.setObject(6, client.getBirthDate());
        statement.setObject(7, client.getRegistrationDate());
        statement.setBoolean(8, client.isActive());
    }

    private Client mapClient(ResultSet resultSet) throws SQLException {
        Client client = new Client();
        client.setId(resultSet.getInt("id"));
        client.setDocumentNumber(resultSet.getString("document_number"));
        client.setFirstName(resultSet.getString("first_name"));
        client.setLastName(resultSet.getString("last_name"));
        client.setPhone(resultSet.getString("phone"));
        client.setEmail(resultSet.getString("email"));
        client.setBirthDate(resultSet.getObject("birth_date", LocalDate.class));
        client.setRegistrationDate(resultSet.getObject("registration_date", LocalDate.class));
        client.setActive(resultSet.getBoolean("active"));
        return client;
    }
}
