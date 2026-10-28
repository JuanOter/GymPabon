package com.gympabon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gympabon.exception.BusinessRuleException;
import com.gympabon.exception.NotFoundException;
import com.gympabon.fake.InMemoryTrainerDAO;
import com.gympabon.fake.TestData;
import com.gympabon.model.Membership;
import com.gympabon.model.Trainer;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests of {@link TrainerService} (RF05, RF11). */
class TrainerServiceTest {

    private TrainerService trainerService;

    @BeforeEach
    void setUp() {
        trainerService = new TrainerService(new InMemoryTrainerDAO());
    }

    @Test
    @DisplayName("Registra un entrenador activo con todos sus cupos libres")
    void registersTrainer() {
        Trainer trainer = trainerService.registerTrainer(TestData.trainer("77123456"));

        assertTrue(trainer.isActive());
        assertEquals(Trainer.MAX_PREMIUM_CLIENTS, trainer.getAvailableSlots());
        assertEquals(1, trainerService.listTrainers().size());
    }

    @Test
    @DisplayName("La especialidad y el horario son obligatorios")
    void requiresSpecialtyAndSchedule() {
        Trainer noSpecialty = TestData.trainer("77123456");
        noSpecialty.setSpecialty("  ");
        Trainer noSchedule = TestData.trainer("77123456");
        noSchedule.setWorkSchedule(null);

        assertThrows(BusinessRuleException.class, () -> trainerService.registerTrainer(noSpecialty));
        assertThrows(BusinessRuleException.class, () -> trainerService.registerTrainer(noSchedule));
    }

    @Test
    @DisplayName("No permite dos entrenadores con el mismo documento")
    void rejectsDuplicateDocument() {
        trainerService.registerTrainer(TestData.trainer("77123456"));

        assertThrows(BusinessRuleException.class, () -> trainerService.registerTrainer(TestData.trainer("77123456")));
    }

    @Test
    @DisplayName("Disponibles: solo activos y con cupo")
    void listsAvailableTrainers() {
        Trainer free = trainerService.registerTrainer(TestData.trainer("77111111"));
        Trainer full = trainerService.registerTrainer(TestData.trainer("77222222"));
        Trainer inactive = trainerService.registerTrainer(TestData.trainer("77333333"));
        for (int i = 0; i < Trainer.MAX_PREMIUM_CLIENTS; i++) {
            full.assignMembership(new Membership());
        }
        trainerService.deactivateTrainer(inactive.getId());

        List<Trainer> available = trainerService.findAvailableTrainers();

        assertEquals(1, available.size());
        assertEquals(free.getId(), available.get(0).getId());
        assertThrows(BusinessRuleException.class, () -> full.assignMembership(new Membership()));
    }

    @Test
    @DisplayName("No se desactiva un entrenador con clientes Premium vigentes")
    void cannotDeactivateTrainerWithClients() {
        Trainer trainer = trainerService.registerTrainer(TestData.trainer("77123456"));
        trainer.assignMembership(new Membership());

        assertThrows(BusinessRuleException.class, () -> trainerService.deactivateTrainer(trainer.getId()));
        assertTrue(trainerService.findById(trainer.getId()).isActive());
    }

    @Test
    @DisplayName("Un entrenador desactivado se puede volver a activar")
    void reactivatesTrainer() {
        Trainer trainer = trainerService.registerTrainer(TestData.trainer("77123456"));
        trainerService.deactivateTrainer(trainer.getId());

        trainerService.activateTrainer(trainer.getId());

        assertTrue(trainerService.findById(trainer.getId()).isActive());
        assertEquals(1, trainerService.findAvailableTrainers().size());
    }

    @Test
    @DisplayName("Actualizar conserva el estado y un id inexistente falla")
    void updatesTrainer() {
        Trainer trainer = trainerService.registerTrainer(TestData.trainer("77123456"));
        trainerService.deactivateTrainer(trainer.getId());
        Trainer changes = TestData.trainer("77123456");
        changes.setId(trainer.getId());
        changes.setSpecialty("Funcional");

        trainerService.updateTrainer(changes);

        Trainer stored = trainerService.findById(trainer.getId());
        assertEquals("Funcional", stored.getSpecialty());
        assertFalse(stored.isActive());
        assertThrows(NotFoundException.class, () -> trainerService.findById(99));
    }
}
