package com.gympabon.exception;

/**
 * Thrown when a requested record does not exist, such as a client with an
 * unregistered document number.
 */
public class NotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }
}
