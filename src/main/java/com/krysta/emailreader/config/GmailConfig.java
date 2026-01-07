package com.krysta.emailreader.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for Gmail API integration.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "gmail")
public class GmailConfig {
    
    /**
     * Application name for Gmail API client
     */
    private String applicationName;
    
    /**
     * Path to the OAuth 2.0 credentials JSON file
     */
    private String credentialsFile;
    
    /**
     * Directory to store OAuth tokens
     */
    private String tokensDirectory;
    
    /**
     * Gmail API scopes required for the application
     */
    private List<String> scopes;
}
