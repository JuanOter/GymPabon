package com.gympabon.fake;

import com.gympabon.dao.TrainerDAO;
import com.gympabon.model.Trainer;
import java.util.List;

/** In-memory {@link TrainerDAO} for unit tests. */
public class InMemoryTrainerDAO extends InMemoryDAO<Trainer> implements TrainerDAO {

    @Override
    protected int idOf(Trainer trainer) {
        return trainer.getId();
    }

    @Override
    protected void assignId(Trainer trainer, int id) {
        trainer.setId(id);
    }

    @Override
    public Trainer findByDocument(String documentNumber) {
        return rows.values().stream().filter(t -> t.getDocumentNumber().equals(documentNumber))
                .findFirst().orElse(null);
    }

    @Override
    public List<Trainer> findActive() {
        return rows.values().stream().filter(Trainer::isActive).toList();
    }
}
