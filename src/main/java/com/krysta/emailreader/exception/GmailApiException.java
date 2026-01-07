package com.krysta.emailreader.exception;

/**
 * Exception thrown when Gmail API operations fail.
 */
public class GmailApiException extends RuntimeException {
    
    public GmailApiException(String message) {
        super(message);
    }
    
    public GmailApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
