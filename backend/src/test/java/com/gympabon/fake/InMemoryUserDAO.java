package com.gympabon.fake;

import com.gympabon.dao.UserDAO;
import com.gympabon.model.User;

/** In-memory {@link UserDAO} for unit tests. */
public class InMemoryUserDAO extends InMemoryDAO<User> implements UserDAO {

    @Override
    protected int idOf(User user) {
        return user.getId();
    }

    @Override
    protected void assignId(User user, int id) {
        user.setId(id);
    }

    @Override
    public User findByUsername(String username) {
        return rows.values().stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
    }
}
