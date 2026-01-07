package com.krysta.emailreader.service;

import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.krysta.emailreader.exception.GmailApiException;

/**
 * Service to store and retrieve Gmail OAuth credentials in memory.
 * Thread-safe storage for credentials provided via API.
 */
@Service
public class CredentialsStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(CredentialsStorageService.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    
    // Thread-safe storage: API key -> credentials JSON
    private final Map<String, String> credentialsStore = new ConcurrentHashMap<>();
    
    // Default key for single-user scenarios
    private static final String DEFAULT_KEY = "default";
    
    /**
     * Store credentials JSON for a given API key.
     * 
     * @param apiKey The API key associated with these credentials
     * @param credentialsJson The complete credentials JSON string
     * @throws GmailApiException if credentials are invalid
     */
    public void storeCredentials(String apiKey, String credentialsJson) {
        if (credentialsJson == null || credentialsJson.trim().isEmpty()) {
            throw new GmailApiException("Credentials JSON cannot be empty");
        }
        
        // Validate JSON format by trying to parse it
        try {
            GoogleClientSecrets.load(JSON_FACTORY, new StringReader(credentialsJson));
            logger.info("Storing credentials for API key: {}", maskApiKey(apiKey));
            credentialsStore.put(apiKey, credentialsJson);
        } catch (Exception e) {
            logger.error("Invalid credentials JSON format", e);
            throw new GmailApiException("Invalid credentials JSON format: " + e.getMessage());
        }
    }
    
    /**
     * Store credentials using default key.
     * 
     * @param credentialsJson The complete credentials JSON string
     */
    public void storeCredentials(String credentialsJson) {
        storeCredentials(DEFAULT_KEY, credentialsJson);
    }
    
    /**
     * Retrieve credentials JSON for a given API key.
     * 
     * @param apiKey The API key
     * @return Credentials JSON string, or null if not found
     */
    public String getCredentials(String apiKey) {
        return credentialsStore.get(apiKey);
    }
    
    /**
     * Retrieve credentials using default key.
     * 
     * @return Credentials JSON string, or null if not found
     */
    public String getCredentials() {
        return credentialsStore.get(DEFAULT_KEY);
    }
    
    /**
     * Check if credentials exist for a given API key.
     * 
     * @param apiKey The API key
     * @return true if credentials exist
     */
    public boolean hasCredentials(String apiKey) {
        return credentialsStore.containsKey(apiKey);
    }
    
    /**
     * Remove credentials for a given API key.
     * 
     * @param apiKey The API key
     */
    public void removeCredentials(String apiKey) {
        logger.info("Removing credentials for API key: {}", maskApiKey(apiKey));
        credentialsStore.remove(apiKey);
    }
    
    /**
     * Clear all stored credentials.
     */
    public void clearAll() {
        logger.info("Clearing all stored credentials");
        credentialsStore.clear();
    }
    
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
    }
}
