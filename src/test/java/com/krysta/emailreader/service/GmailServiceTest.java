package com.krysta.emailreader.service;

import com.krysta.emailreader.config.GmailConfig;
import com.krysta.emailreader.exception.GmailApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GmailService.
 * Note: These tests require proper Gmail API setup to run integration tests.
 * For unit tests, we're testing the configuration and error handling.
 */
@ExtendWith(MockitoExtension.class)
class GmailServiceTest {
    
    @Mock
    private GmailConfig gmailConfig;
    
    @Mock
    private CredentialsStorageService credentialsStorageService;
    
    private GmailService gmailService;
    
    @BeforeEach
    void setUp() {
        gmailConfig = new GmailConfig();
        gmailConfig.setApplicationName("Test Application");
        gmailConfig.setCredentialsFile("/credentials.json");
        gmailConfig.setTokensDirectory("tokens");
        gmailConfig.setScopes(List.of("https://www.googleapis.com/auth/gmail.readonly"));
        
        gmailService = new GmailService(gmailConfig, credentialsStorageService);
    }
    
    @Test
    void testGmailService_InitializationWithoutCredentials_ThrowsException() {
        // This test verifies that the service handles missing credentials gracefully
        // In a real scenario, calling countEmailsFromSender without credentials should throw GmailApiException
        
        String senderEmail = "test@example.com";
        
        // Act & Assert
        assertThrows(GmailApiException.class, () -> {
            gmailService.countEmailsFromSender(senderEmail);
        });
    }
    
    @Test
    void testGmailConfig_PropertiesAreSet() {
        // Verify configuration is properly set
        assertEquals("Test Application", gmailConfig.getApplicationName());
        assertEquals("/credentials.json", gmailConfig.getCredentialsFile());
        assertEquals("tokens", gmailConfig.getTokensDirectory());
        assertNotNull(gmailConfig.getScopes());
        assertTrue(gmailConfig.getScopes().contains("https://www.googleapis.com/auth/gmail.readonly"));
    }
}
