package com.gympabon.exception;

/**
 * Thrown when a request needs a signed-in user and there is none, or the
 * session is no longer valid. The API answers it with HTTP 401.
 */
public class NotAuthenticatedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotAuthenticatedException(String message) {
        super(message);
    }
}
