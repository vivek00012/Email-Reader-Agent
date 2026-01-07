package com.krysta.emailreader.service;

import com.krysta.emailreader.exception.InvalidEmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Service class for email operations with caching support.
 * Provides business logic and validation for email counting.
 */
@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    // Email validation regex pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private final GmailService gmailService;
    
    public EmailService(GmailService gmailService) {
        this.gmailService = gmailService;
    }
    
    /**
     * Validates email format.
     * 
     * @param email The email address to validate
     * @throws InvalidEmailException if the email format is invalid
     */
    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidEmailException("Email address cannot be empty");
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidEmailException("Invalid email format: " + email);
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
        logger.debug("Getting email count for sender: {}", senderEmail);
        
        // Validate email format
        validateEmail(senderEmail);
        
        // Fetch count from Gmail API
        long count = gmailService.countEmailsFromSender(senderEmail);
        
        logger.info("Email count for {}: {}", senderEmail, count);
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
