package com.gympabon.fake;

import com.gympabon.dao.GenericDAO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base of the in-memory DAOs: keeps the entities in a map and assigns
 * consecutive identifiers, like AUTO_INCREMENT in MySQL.
 *
 * @param <T> entity type
 */
public abstract class InMemoryDAO<T> implements GenericDAO<T, Integer> {

    protected final Map<Integer, T> rows = new LinkedHashMap<>();
    private int nextId = 1;

    /**
     * Returns the identifier of an entity.
     *
     * @param entity entity
     * @return its identifier
     */
    protected abstract int idOf(T entity);

    /**
     * Sets the identifier generated for a new entity.
     *
     * @param entity entity
     * @param id     generated identifier
     */
    protected abstract void assignId(T entity, int id);

    /**
     * Returns the object that is stored or handed out. By default it is the
     * same object; a subclass returns a copy when, like the real DAO, every
     * read must give a new object.
     *
     * @param entity entity
     * @return the entity or a copy of it
     */
    protected T copy(T entity) {
        return entity;
    }

    @Override
    public void save(T entity) {
        assignId(entity, nextId++);
        rows.put(idOf(entity), copy(entity));
    }

    @Override
    public void update(T entity) {
        if (!rows.containsKey(idOf(entity))) {
            throw new IllegalStateException("No existe el registro " + idOf(entity));
        }
        rows.put(idOf(entity), copy(entity));
    }

    @Override
    public T findById(Integer id) {
        T entity = rows.get(id);
        return entity == null ? null : copy(entity);
    }

    @Override
    public List<T> findAll() {
        List<T> list = new ArrayList<>();
        rows.values().forEach(entity -> list.add(copy(entity)));
        return list;
    }

    @Override
    public void delete(Integer id) {
        rows.remove(id);
    }

    /**
     * Number of stored entities, useful in the assertions.
     *
     * @return number of rows
     */
    public int count() {
        return rows.size();
    }
}
