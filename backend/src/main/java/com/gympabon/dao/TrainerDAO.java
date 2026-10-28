package com.gympabon.dao;

import com.gympabon.model.Trainer;
import java.util.List;

/**
 * Data access for trainers. Each trainer is loaded with its current Premium
 * memberships so it can calculate its free slots.
 */
public interface TrainerDAO extends GenericDAO<Trainer, Integer> {

    /**
     * Finds a trainer by document number.
     *
     * @param documentNumber document to look for
     * @return the trainer, or null if there is no trainer with that document
     */
    Trainer findByDocument(String documentNumber);

    /**
     * Returns the trainers that are active.
     *
     * @return the active trainers ordered by name
     */
    List<Trainer> findActive();
}
