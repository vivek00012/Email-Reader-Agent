package com.krysta.emailreader.service;

import com.krysta.emailreader.exception.InvalidEmailException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    
    @Mock
    private GmailService gmailService;
    
    @InjectMocks
    private EmailService emailService;
    
    @Test
    void testGetEmailCount_ValidEmail_ReturnsCount() {
        // Arrange
        String senderEmail = "superman@example.com";
        long expectedCount = 10L;
        when(gmailService.countEmailsFromSender(senderEmail)).thenReturn(expectedCount);
        
        // Act
        long actualCount = emailService.getEmailCount(senderEmail);
        
        // Assert
        assertEquals(expectedCount, actualCount);
        verify(gmailService, times(1)).countEmailsFromSender(senderEmail);
    }
    
    @Test
    void testGetEmailCount_InvalidEmailFormat_ThrowsException() {
        // Arrange
        String invalidEmail = "not-an-email";
        
        // Act & Assert
        assertThrows(InvalidEmailException.class, () -> {
            emailService.getEmailCount(invalidEmail);
        });
        
        verify(gmailService, never()).countEmailsFromSender(anyString());
    }
    
    @Test
    void testGetEmailCount_EmptyEmail_ThrowsException() {
        // Arrange
        String emptyEmail = "";
        
        // Act & Assert
        assertThrows(InvalidEmailException.class, () -> {
            emailService.getEmailCount(emptyEmail);
        });
        
        verify(gmailService, never()).countEmailsFromSender(anyString());
    }
    
    @Test
    void testGetEmailCount_NullEmail_ThrowsException() {
        // Arrange
        String nullEmail = null;
        
        // Act & Assert
        assertThrows(InvalidEmailException.class, () -> {
            emailService.getEmailCount(nullEmail);
        });
        
        verify(gmailService, never()).countEmailsFromSender(anyString());
    }
    
    @Test
    void testGetEmailCount_ValidEmailWithNumbers_ReturnsCount() {
        // Arrange
        String senderEmail = "user123@example.com";
        long expectedCount = 5L;
        when(gmailService.countEmailsFromSender(senderEmail)).thenReturn(expectedCount);
        
        // Act
        long actualCount = emailService.getEmailCount(senderEmail);
        
        // Assert
        assertEquals(expectedCount, actualCount);
        verify(gmailService, times(1)).countEmailsFromSender(senderEmail);
    }
}
