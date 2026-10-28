package com.gympabon.dao.impl;

import com.gympabon.dao.PlanDAO;
import com.gympabon.exception.DataAccessException;
import com.gympabon.model.BasicPlan;
import com.gympabon.model.Plan;
import com.gympabon.model.PremiumPlan;
import com.gympabon.model.StandardPlan;
import com.gympabon.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of {@link PlanDAO}. The plan_type column decides which
 * subclass of {@link Plan} is created for each row.
 */
@Repository
public class PlanDAOImpl implements PlanDAO {

    private static final String SELECT_PLAN = "SELECT id, plan_type, name, monthly_price, description FROM plan";

    private final DatabaseConnection databaseConnection;

    public PlanDAOImpl(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public void save(Plan plan) {
        String sql = "INSERT INTO plan (plan_type, name, monthly_price, description) VALUES (?, ?, ?, ?)";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, planTypeOf(plan));
            statement.setString(2, plan.getName());
            statement.setDouble(3, plan.getMonthlyPrice());
            statement.setString(4, plan.getDescription());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    plan.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible guardar el plan", e);
        }
    }

    @Override
    public void update(Plan plan) {
        String sql = "UPDATE plan SET name = ?, monthly_price = ?, description = ? WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, plan.getName());
            statement.setDouble(2, plan.getMonthlyPrice());
            statement.setString(3, plan.getDescription());
            statement.setInt(4, plan.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible actualizar el plan", e);
        }
    }

    @Override
    public Plan findById(Integer id) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PLAN + " WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapPlan(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible consultar el plan", e);
        }
    }

    @Override
    public List<Plan> findAll() {
        List<Plan> plans = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PLAN + " ORDER BY monthly_price");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                plans.add(mapPlan(resultSet));
            }
            return plans;
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible consultar los planes", e);
        }
    }

    @Override
    public void delete(Integer id) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM plan WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible eliminar el plan; puede tener membresías asociadas", e);
        }
    }

    private Plan mapPlan(ResultSet resultSet) throws SQLException {
        return createPlan(resultSet.getString("plan_type"), resultSet.getInt("id"), resultSet.getString("name"),
                resultSet.getDouble("monthly_price"), resultSet.getString("description"));
    }

    /**
     * Creates the subclass of {@link Plan} that matches the plan_type column.
     * Other DAOs of this package use it when they load a plan with a JOIN.
     *
     * @param planType    value of the plan_type column: BASIC, STANDARD or PREMIUM
     * @param id          plan identifier
     * @param name        plan name
     * @param price       monthly price
     * @param description plan description
     * @return the plan object of the right subclass
     */
    static Plan createPlan(String planType, int id, String name, double price, String description) {
        return switch (planType) {
            case "BASIC" -> new BasicPlan(id, name, price, description);
            case "STANDARD" -> new StandardPlan(id, name, price, description);
            case "PREMIUM" -> new PremiumPlan(id, name, price, description);
            default -> throw new IllegalArgumentException("Tipo de plan desconocido: " + planType);
        };
    }

    private static String planTypeOf(Plan plan) {
        if (plan instanceof BasicPlan) {
            return "BASIC";
        }
        if (plan instanceof PremiumPlan) {
            return "PREMIUM";
        }
        return "STANDARD";
    }
}
