package com.gympabon.exception;

/**
 * Thrown when an operation breaks one of the gym's business rules, for example
 * a Basic plan client trying to enter on a Saturday or a trainer without free
 * Premium slots. The message is shown to the user as is.
 */
public class BusinessRuleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }
}
