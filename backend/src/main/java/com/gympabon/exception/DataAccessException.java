package com.gympabon.exception;

/**
 * Wraps JDBC and MySQL errors so that the upper layers do not depend on
 * {@link java.sql.SQLException}.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
