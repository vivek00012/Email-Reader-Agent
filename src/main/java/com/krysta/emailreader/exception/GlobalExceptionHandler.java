package com.krysta.emailreader.exception;

import com.krysta.emailreader.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

/**
 * Global exception handler for centralized error handling across the application.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle InvalidEmailException - 400 Bad Request
     */
    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEmailException(
            InvalidEmailException ex, WebRequest request) {
        logger.warn("Invalid email format: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle GmailApiException - 500 Internal Server Error or appropriate status
     */
    @ExceptionHandler(GmailApiException.class)
    public ResponseEntity<ErrorResponse> handleGmailApiException(
            GmailApiException ex, WebRequest request) {
        logger.error("Gmail API error: {}", ex.getMessage(), ex);
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Failed to communicate with Gmail API: " + ex.getMessage();
        
        // Check if it's an authentication issue
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("auth")) {
            status = HttpStatus.UNAUTHORIZED;
            message = "Gmail authentication failed. Please check your credentials.";
        }
        // Check if it's a rate limit issue
        else if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("rate")) {
            status = HttpStatus.TOO_MANY_REQUESTS;
            message = "Gmail API rate limit exceeded. Please try again later.";
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(errorResponse, status);
    }
    
    /**
     * Handle all other exceptions - 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An unexpected error occurred: " + ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
