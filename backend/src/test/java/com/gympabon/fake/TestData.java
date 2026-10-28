package com.gympabon.fake;

import com.gympabon.model.BasicPlan;
import com.gympabon.model.Client;
import com.gympabon.model.Plan;
import com.gympabon.model.PremiumPlan;
import com.gympabon.model.Role;
import com.gympabon.model.StandardPlan;
import com.gympabon.model.Trainer;
import com.gympabon.model.User;
import java.time.LocalDate;

/**
 * Builds the objects that the tests need, with the same plans and prices of
 * the seed data (database/seed.sql).
 */
public final class TestData {

    public static final double BASIC_PRICE = 70000;
    public static final double STANDARD_PRICE = 95000;
    public static final double PREMIUM_PRICE = 140000;

    private TestData() {
    }

    /**
     * Saves the three plans of the gym: 1 Básico, 2 Estándar and 3 Premium.
     *
     * @param planDAO DAO where the plans are saved
     */
    public static void savePlans(InMemoryPlanDAO planDAO) {
        planDAO.save(basicPlan());
        planDAO.save(standardPlan());
        planDAO.save(premiumPlan());
    }

    public static Plan basicPlan() {
        return new BasicPlan(0, "Básico", BASIC_PRICE, "Lunes a viernes de 5:00 a. m. a 3:00 p. m.");
    }

    public static Plan standardPlan() {
        return new StandardPlan(0, "Estándar", STANDARD_PRICE, "Todo el horario del gimnasio");
    }

    public static Plan premiumPlan() {
        return new PremiumPlan(0, "Premium", PREMIUM_PRICE, "Todo el horario con entrenador personal");
    }

    /**
     * Creates a valid client that is not saved yet.
     *
     * @param document document number (6 to 12 digits)
     * @return the client
     */
    public static Client client(String document) {
        return new Client(document, "Andrés", "Pérez", "3001234567", "andres@correo.com",
                LocalDate.now().minusYears(25), null);
    }

    /**
     * Creates a valid trainer that is not saved yet.
     *
     * @param document document number (6 to 12 digits)
     * @return the trainer
     */
    public static Trainer trainer(String document) {
        return new Trainer(document, "Laura", "Gómez", "3109876543", "laura@gympabon.com",
                "Fuerza", "Lunes a viernes 6:00 a. m. - 2:00 p. m.");
    }

    /**
     * Creates a user with the given role, not saved and without password.
     *
     * @param username username
     * @param role     role of the account
     * @return the user
     */
    public static User user(String username, Role role) {
        return new User("1065000111", "Juan", "Otero", "3001112233", "juan@gympabon.com", username, null, role);
    }
}
