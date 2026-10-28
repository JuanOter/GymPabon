package com.gympabon.dao;

import com.gympabon.model.User;

/**
 * Data access for the system user accounts.
 */
public interface UserDAO extends GenericDAO<User, Integer> {

    /**
     * Finds a user by username.
     *
     * @param username username to look for
     * @return the user, or null if there is no account with that username
     */
    User findByUsername(String username);
}
