package com.krysta.emailreader.exception;

/**
 * Exception thrown when an invalid email format is provided.
 */
public class InvalidEmailException extends RuntimeException {
    
    public InvalidEmailException(String message) {
        super(message);
    }
}
