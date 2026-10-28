package com.gympabon.dao.impl;

import com.gympabon.dao.UserDAO;
import com.gympabon.exception.DataAccessException;
import com.gympabon.model.Role;
import com.gympabon.model.User;
import com.gympabon.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of {@link UserDAO}. The table is called app_user because
 * "user" is a reserved word in MySQL.
 */
@Repository
public class UserDAOImpl implements UserDAO {

    private static final String SELECT_USER = """
            SELECT id, document_number, first_name, last_name, phone, email, username, password_hash,
                   role, active, last_login
            FROM app_user
            """;

    private final DatabaseConnection databaseConnection;

    public UserDAOImpl(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public void save(User user) {
        String sql = """
                INSERT INTO app_user (document_number, first_name, last_name, phone, email, username,
                                      password_hash, role, active, last_login)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillStatement(statement, user);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible guardar el usuario", e);
        }
    }

    @Override
    public void update(User user) {
        String sql = """
                UPDATE app_user SET document_number = ?, first_name = ?, last_name = ?, phone = ?, email = ?,
                       username = ?, password_hash = ?, role = ?, active = ?, last_login = ?
                WHERE id = ?
                """;
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, user);
            statement.setInt(11, user.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible actualizar el usuario", e);
        }
    }

    @Override
    public User findById(Integer id) {
        List<User> result = query(SELECT_USER + " WHERE id = ?", id);
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<User> findAll() {
        return query(SELECT_USER + " ORDER BY first_name, last_name");
    }

    @Override
    public void delete(Integer id) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM app_user WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible eliminar el usuario; puede tener registros asociados", e);
        }
    }

    @Override
    public User findByUsername(String username) {
        List<User> result = query(SELECT_USER + " WHERE username = ?", username);
        return result.isEmpty() ? null : result.get(0);
    }

    private List<User> query(String sql, Object... parameters) {
        List<User> users = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapUser(resultSet));
                }
            }
            return users;
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible consultar los usuarios", e);
        }
    }

    private void fillStatement(PreparedStatement statement, User user) throws SQLException {
        statement.setString(1, user.getDocumentNumber());
        statement.setString(2, user.getFirstName());
        statement.setString(3, user.getLastName());
        statement.setString(4, user.getPhone());
        statement.setString(5, user.getEmail());
        statement.setString(6, user.getUsername());
        statement.setString(7, user.getPasswordHash());
        statement.setString(8, user.getRole().name());
        statement.setBoolean(9, user.isActive());
        statement.setObject(10, user.getLastLogin());
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getInt("id"));
        user.setDocumentNumber(resultSet.getString("document_number"));
        user.setFirstName(resultSet.getString("first_name"));
        user.setLastName(resultSet.getString("last_name"));
        user.setPhone(resultSet.getString("phone"));
        user.setEmail(resultSet.getString("email"));
        user.setUsername(resultSet.getString("username"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setRole(Role.valueOf(resultSet.getString("role")));
        user.setActive(resultSet.getBoolean("active"));
        user.setLastLogin(resultSet.getObject("last_login", LocalDateTime.class));
        return user;
    }
}
