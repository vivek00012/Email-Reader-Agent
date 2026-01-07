package com.krysta.emailreader.exception;

import java.io.IOException;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.krysta.emailreader.dto.ErrorResponse;
import com.krysta.emailreader.service.AuditService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler for centralized error handling across the application.
 * Implements secure error handling with correlation IDs and sanitized messages.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    private final AuditService auditService;
    
    @Value("${security.detailed-errors:false}")
    private boolean detailedErrors;
    
    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }
    
    /**
     * Handle InvalidEmailException - 400 Bad Request
     */
    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEmailException(
            InvalidEmailException ex, WebRequest request, HttpServletRequest httpRequest) {
        String correlationId = generateCorrelationId();
        
        logger.warn("[{}] Invalid email format: {}", correlationId, ex.getMessage());
        
        // Audit log validation failure
        String clientIp = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
        auditService.logValidationFailure(clientIp, "email", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(detailedErrors ? ex.getMessage() : "Invalid email format provided")
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
        // Check if it's a cancellation (client disconnected)
        if (ex.getMessage() != null && ex.getMessage().contains("cancelled")) {
            logger.debug("Request cancelled: {}", ex.getMessage());
            return null;
        }
        
        String correlationId = generateCorrelationId();
        
        // Log detailed error server-side with correlation ID
        logger.error("[{}] Gmail API error: {}", correlationId, ex.getMessage(), ex);
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "An error occurred while processing your request";
        
        String errorMsg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        
        // Check if it's a credentials missing issue
        if (errorMsg.contains("credentials not found")) {
            status = HttpStatus.BAD_REQUEST;
            message = ex.getMessage();
        }
        // Check if it's a port conflict (before auth check to avoid misclassification)
        else if (errorMsg.contains("port") && errorMsg.contains("in use")) {
            status = HttpStatus.BAD_REQUEST;
            message = ex.getMessage();
        }
        // Check if it's an authentication issue (exclude OAuth/port messages)
        else if (errorMsg.contains("auth") && !errorMsg.contains("oauth") && !errorMsg.contains("port")) {
            status = HttpStatus.UNAUTHORIZED;
            message = "Gmail authentication failed";
        }
        // Check if it's a rate limit issue
        else if (errorMsg.contains("rate")) {
            status = HttpStatus.TOO_MANY_REQUESTS;
            message = "Gmail API rate limit exceeded. Please try again later";
        }
        
        // In production, don't expose internal error details
        if (!detailedErrors) {
            message = getSanitizedMessage(message);
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
     * Handle client disconnection exceptions gracefully.
     * This happens when the client cancels the request (e.g., in Swagger UI).
     */
    @ExceptionHandler({ClientAbortException.class})
    public ResponseEntity<ErrorResponse> handleClientAbortException(
            ClientAbortException ex, WebRequest request) {
        // Log at debug level since this is expected behavior when clients cancel requests
        logger.debug("Client disconnected: {}", ex.getMessage());
        
        // Return null to indicate the response was already sent or connection closed
        // This prevents further processing and app crash
        return null;
    }
    
    /**
     * Handle IOException that may occur due to client disconnection.
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(
            IOException ex, WebRequest request) {
        // Check if it's a client disconnection (broken pipe, connection reset)
        String message = ex.getMessage();
        if (message != null && (
            message.contains("Broken pipe") ||
            message.contains("Connection reset") ||
            message.contains("Connection closed") ||
            message.contains("Socket closed")
        )) {
            logger.debug("Client disconnected: {}", message);
            // Return null to prevent further processing
            return null;
        }
        
        // For other IOExceptions, handle normally
        String correlationId = generateCorrelationId();
        logger.error("[{}] IO error: {}", correlationId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An error occurred while processing your request")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Handle SocketException (connection reset, etc.)
     */
    @ExceptionHandler(SocketException.class)
    public ResponseEntity<ErrorResponse> handleSocketException(
            SocketException ex, WebRequest request) {
        String message = ex.getMessage();
        if (message != null && (
            message.contains("Connection reset") ||
            message.contains("Socket closed")
        )) {
            logger.debug("Client disconnected: {}", message);
            return null;
        }
        
        // Handle other socket exceptions
        String correlationId = generateCorrelationId();
        logger.error("[{}] Socket error: {}", correlationId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Network error occurred")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Handle InterruptedException (thread interruption)
     */
    @ExceptionHandler(InterruptedException.class)
    public ResponseEntity<ErrorResponse> handleInterruptedException(
            InterruptedException ex, WebRequest request) {
        logger.debug("Request interrupted: {}", ex.getMessage());
        // Restore interrupted status
        Thread.currentThread().interrupt();
        return null;
    }
    
    /**
     * Handle all other exceptions - 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        // Skip handling if it's a client disconnection
        if (ex instanceof ClientAbortException ||
            (ex instanceof IOException && ex.getMessage() != null && 
             (ex.getMessage().contains("Broken pipe") || 
              ex.getMessage().contains("Connection reset")))) {
            logger.debug("Client disconnected: {}", ex.getMessage());
            return null;
        }
        
        String correlationId = generateCorrelationId();
        
        // Log detailed error server-side with correlation ID
        logger.error("[{}] Unexpected error: {}", correlationId, ex.getMessage(), ex);
        
        // Never expose internal error details in production
        String message = detailedErrors ? 
            "An unexpected error occurred: " + ex.getClass().getSimpleName() :
            "An unexpected error occurred. Please contact support with correlation ID: " + correlationId;
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Generate a unique correlation ID for error tracking.
     */
    private String generateCorrelationId() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        return correlationId;
    }
    
    /**
     * Sanitize error messages to prevent information leakage.
     */
    private String getSanitizedMessage(String message) {
        // Remove any potential sensitive information
        return message.replaceAll("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b", "[IP]")
                     .replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", "[EMAIL]")
                     .replaceAll("(?i)(password|token|key|secret)=\\S+", "$1=[REDACTED]");
    }
}
