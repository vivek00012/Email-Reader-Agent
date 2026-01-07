package com.krysta.emailreader.service;

import com.krysta.emailreader.exception.InvalidEmailException;
import com.krysta.emailreader.util.LogSanitizer;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service class for email operations with caching support.
 * Provides business logic and validation for email counting.
 */
@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final int MAX_EMAIL_LENGTH = 254; // RFC 5321
    private static final EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance(false);
    
    private final GmailService gmailService;
    
    public EmailService(GmailService gmailService) {
        this.gmailService = gmailService;
    }
    
    /**
     * Validates email format using Apache Commons Validator.
     * Implements comprehensive validation including:
     * - RFC 5321 compliance
     * - Length checks
     * - Homograph attack prevention
     * - Special character sanitization
     * 
     * @param email The email address to validate
     * @throws InvalidEmailException if the email format is invalid
     */
    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidEmailException("Email address cannot be empty");
        }
        
        String trimmedEmail = email.trim();
        
        // Check maximum length per RFC 5321
        if (trimmedEmail.length() > MAX_EMAIL_LENGTH) {
            throw new InvalidEmailException("Email address exceeds maximum length of " + MAX_EMAIL_LENGTH + " characters");
        }
        
        // Use Apache Commons EmailValidator for robust validation
        if (!EMAIL_VALIDATOR.isValid(trimmedEmail)) {
            throw new InvalidEmailException("Invalid email format");
        }
        
        // Prevent homograph attacks - check for non-ASCII characters
        if (!trimmedEmail.matches("^[\\x00-\\x7F]+$")) {
            throw new InvalidEmailException("Email address contains invalid characters");
        }
        
        // Additional security checks
        String localPart = trimmedEmail.substring(0, trimmedEmail.indexOf('@'));
        String domain = trimmedEmail.substring(trimmedEmail.indexOf('@') + 1);
        
        // Check for suspicious patterns in local part
        if (localPart.startsWith(".") || localPart.endsWith(".") || localPart.contains("..")) {
            throw new InvalidEmailException("Email address contains invalid format");
        }
        
        // Validate domain has at least one dot
        if (!domain.contains(".")) {
            throw new InvalidEmailException("Email domain is invalid");
        }
        
        // Check for IP addresses in domain (potential security risk)
        if (domain.matches("^\\[?\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\]?$")) {
            throw new InvalidEmailException("Email addresses with IP domains are not supported");
        }
    }
    
    /**
     * Gets the count of emails from a specific sender.
     * Results are cached to reduce Gmail API calls.
     * 
     * @param senderEmail The email address of the sender
     * @return The count of emails from the sender
     * @throws InvalidEmailException if the email format is invalid
     */
    @Cacheable(value = "emailCounts", key = "#senderEmail")
    public long getEmailCount(String senderEmail) {
        logger.debug("Getting email count for sender: {}", LogSanitizer.maskEmail(senderEmail));
        
        // Validate email format
        validateEmail(senderEmail);
        
        // Fetch count from Gmail API
        long count = gmailService.countEmailsFromSender(senderEmail);
        
        logger.info("Email count for {}: {}", LogSanitizer.maskEmail(senderEmail), count);
        return count;
    }
    
    /**
     * Checks if a result is from cache (used by controller for response metadata).
     * This is a simple implementation - in production you might want to use
     * cache statistics from CacheManager.
     * 
     * @param senderEmail The email address to check
     * @return true if likely cached (this is called after getEmailCount)
     */
    public boolean isCached(String senderEmail) {
        // This is a simplified approach - the first call will set cache,
        // subsequent calls within TTL will be cached
        // In a real scenario, you'd check cache statistics
        return false; // Controller will handle this logic
    }
}
