package com.krysta.emailreader.service;

import com.krysta.emailreader.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for security audit logging.
 * Tracks all security-relevant events for compliance and monitoring.
 */
@Service
public class AuditService {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Log successful API access.
     */
    public void logApiAccess(String endpoint, String ipAddress, String email) {
        auditLogger.info("API_ACCESS | timestamp={} | endpoint={} | ip={} | email={} | status=SUCCESS",
            LocalDateTime.now().format(formatter),
            endpoint,
            LogSanitizer.maskIpAddress(ipAddress),
            LogSanitizer.maskEmail(email)
        );
    }
    
    /**
     * Log failed authentication attempt.
     */
    public void logAuthenticationFailure(String ipAddress, String reason) {
        auditLogger.warn("AUTH_FAILURE | timestamp={} | ip={} | reason={}",
            LocalDateTime.now().format(formatter),
            LogSanitizer.maskIpAddress(ipAddress),
            reason
        );
    }
    
    /**
     * Log successful authentication.
     */
    public void logAuthenticationSuccess(String ipAddress) {
        auditLogger.info("AUTH_SUCCESS | timestamp={} | ip={}",
            LocalDateTime.now().format(formatter),
            LogSanitizer.maskIpAddress(ipAddress)
        );
    }
    
    /**
     * Log rate limit violation.
     */
    public void logRateLimitViolation(String ipAddress, String endpoint) {
        auditLogger.warn("RATE_LIMIT_EXCEEDED | timestamp={} | ip={} | endpoint={}",
            LocalDateTime.now().format(formatter),
            LogSanitizer.maskIpAddress(ipAddress),
            endpoint
        );
    }
    
    /**
     * Log input validation failure.
     */
    public void logValidationFailure(String ipAddress, String field, String reason) {
        auditLogger.warn("VALIDATION_FAILURE | timestamp={} | ip={} | field={} | reason={}",
            LocalDateTime.now().format(formatter),
            LogSanitizer.maskIpAddress(ipAddress),
            field,
            reason
        );
    }
    
    /**
     * Log Gmail API errors.
     */
    public void logGmailApiError(String email, String errorType) {
        auditLogger.error("GMAIL_API_ERROR | timestamp={} | email={} | error={}",
            LocalDateTime.now().format(formatter),
            LogSanitizer.maskEmail(email),
            errorType
        );
    }
    
    /**
     * Log security events (e.g., suspicious activity).
     */
    public void logSecurityEvent(String eventType, String ipAddress, String details) {
        auditLogger.warn("SECURITY_EVENT | timestamp={} | type={} | ip={} | details={}",
            LocalDateTime.now().format(formatter),
            eventType,
            LogSanitizer.maskIpAddress(ipAddress),
            details
        );
    }
    
    /**
     * Log cache operations for monitoring.
     */
    public void logCacheOperation(String operation, String email, boolean hit) {
        auditLogger.debug("CACHE_OPERATION | timestamp={} | operation={} | email={} | hit={}",
            LocalDateTime.now().format(formatter),
            operation,
            LogSanitizer.maskEmail(email),
            hit
        );
    }
}
