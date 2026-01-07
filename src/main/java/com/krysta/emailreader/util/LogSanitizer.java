package com.krysta.emailreader.util;

/**
 * Utility class for sanitizing sensitive information in logs.
 */
public class LogSanitizer {
    
    private LogSanitizer() {
        // Utility class
    }
    
    /**
     * Masks an email address to protect PII in logs.
     * Example: superman@example.com → su****@example.com
     * 
     * @param email The email address to mask
     * @return Masked email address
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "[EMPTY]";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "[INVALID_EMAIL]";
        }
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (localPart.length() <= 2) {
            return "**" + domain;
        }
        
        return localPart.substring(0, 2) + "****" + domain;
    }
    
    /**
     * Masks an IP address to protect privacy in logs.
     * Example: 192.168.1.100 → 192.168.*.*
     * 
     * @param ip The IP address to mask
     * @return Masked IP address
     */
    public static String maskIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "[EMPTY]";
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            // IPv6 or invalid format
            return "[MASKED_IP]";
        }
        
        return parts[0] + "." + parts[1] + ".*.*";
    }
    
    /**
     * Masks sensitive data like API keys, tokens, passwords.
     * 
     * @param value The value to mask
     * @return Masked value showing only first 4 chars
     */
    public static String maskSensitiveValue(String value) {
        if (value == null || value.isEmpty()) {
            return "[EMPTY]";
        }
        
        if (value.length() <= 4) {
            return "****";
        }
        
        return value.substring(0, 4) + "****";
    }
}
