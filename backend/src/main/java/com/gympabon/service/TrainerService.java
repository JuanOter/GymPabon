package com.gympabon.service;

import com.gympabon.dao.TrainerDAO;
import com.gympabon.exception.BusinessRuleException;
import com.gympabon.exception.NotFoundException;
import com.gympabon.model.Trainer;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Business rules for the gym trainers and their Premium client slots.
 */
@Service
public class TrainerService {

    private final TrainerDAO trainerDAO;

    public TrainerService(TrainerDAO trainerDAO) {
        this.trainerDAO = trainerDAO;
    }

    /**
     * Validates the trainer data and saves it as an active trainer.
     *
     * @param trainer new trainer
     * @return the saved trainer with its identifier
     * @throws BusinessRuleException if the data is invalid or the document is already registered
     */
    public Trainer registerTrainer(Trainer trainer) {
        validate(trainer);
        ensureDocumentIsFree(trainer.getDocumentNumber(), 0);
        trainer.setActive(true);
        trainerDAO.save(trainer);
        return trainer;
    }

    /**
     * Validates and saves the changes of an existing trainer.
     *
     * @param trainer trainer with its identifier and the new data
     * @throws NotFoundException     if the trainer does not exist
     * @throws BusinessRuleException if the data is invalid or the document belongs to another trainer
     */
    public void updateTrainer(Trainer trainer) {
        Trainer stored = findById(trainer.getId());
        validate(trainer);
        ensureDocumentIsFree(trainer.getDocumentNumber(), trainer.getId());
        trainer.setActive(stored.isActive());
        trainerDAO.update(trainer);
    }

    /**
     * Finds a trainer by its identifier, with its current Premium memberships.
     *
     * @param trainerId identifier of the trainer
     * @return the trainer
     * @throws NotFoundException if the trainer does not exist
     */
    public Trainer findById(int trainerId) {
        Trainer trainer = trainerDAO.findById(trainerId);
        if (trainer == null) {
            throw new NotFoundException("El entrenador no existe");
        }
        return trainer;
    }

    /**
     * Returns every trainer with its free slots.
     *
     * @return all the trainers
     */
    public List<Trainer> listTrainers() {
        return trainerDAO.findAll();
    }

    /**
     * Returns the active trainers that can still receive a Premium client.
     *
     * @return the trainers with at least one free slot
     */
    public List<Trainer> findAvailableTrainers() {
        return trainerDAO.findActive().stream()
                .filter(Trainer::hasAvailableSlot)
                .toList();
    }

    /**
     * Deactivates a trainer that has no current Premium clients.
     *
     * @param trainerId identifier of the trainer
     * @throws NotFoundException     if the trainer does not exist
     * @throws BusinessRuleException if the trainer still has current Premium clients
     */
    public void deactivateTrainer(int trainerId) {
        Trainer trainer = findById(trainerId);
        int currentClients = trainer.getPremiumMemberships().size();
        if (currentClients > 0) {
            throw new BusinessRuleException("El entrenador tiene " + currentClients
                    + " cliente(s) Premium vigentes; no se puede desactivar");
        }
        trainer.setActive(false);
        trainerDAO.update(trainer);
    }

    /**
     * Activates again a trainer that was deactivated.
     *
     * @param trainerId identifier of the trainer
     * @throws NotFoundException if the trainer does not exist
     */
    public void activateTrainer(int trainerId) {
        Trainer trainer = findById(trainerId);
        trainer.setActive(true);
        trainerDAO.update(trainer);
    }

    private void validate(Trainer trainer) {
        PersonValidator.validate(trainer);
        trainer.setSpecialty(PersonValidator.requireText(trainer.getSpecialty(), "La especialidad"));
        trainer.setWorkSchedule(PersonValidator.requireText(trainer.getWorkSchedule(), "El horario"));
    }

    private void ensureDocumentIsFree(String documentNumber, int trainerId) {
        Trainer existing = trainerDAO.findByDocument(documentNumber);
        if (existing != null && existing.getId() != trainerId) {
            throw new BusinessRuleException("Ya existe un entrenador con el documento " + documentNumber);
        }
    }
}
