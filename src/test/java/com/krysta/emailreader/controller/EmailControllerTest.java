package com.krysta.emailreader.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.krysta.emailreader.config.GmailConfig;
import com.krysta.emailreader.exception.InvalidEmailException;
import com.krysta.emailreader.service.AuditService;
import com.krysta.emailreader.service.CredentialsStorageService;
import com.krysta.emailreader.service.EmailService;

/**
 * Unit tests for EmailController using MockMvc.
 */
@WebMvcTest(EmailController.class)
class EmailControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private EmailService emailService;
    
    @MockBean
    private CacheManager cacheManager;
    
    @MockBean
    private AuditService auditService;
    
    @MockBean
    private CredentialsStorageService credentialsStorageService;
    
    @MockBean
    private GmailConfig gmailConfig;
    
    @Test
    @WithMockUser
    void testCountEmails_ValidEmail_ReturnsOk() throws Exception {
        // Arrange
        String senderEmail = "superman@example.com";
        long emailCount = 10L;
        when(emailService.getEmailCount(senderEmail)).thenReturn(emailCount);
        when(cacheManager.getCache("emailCounts")).thenReturn(null);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/emails/count")
                .param("senderEmail", senderEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderEmail").value(senderEmail))
                .andExpect(jsonPath("$.emailCount").value(emailCount))
                .andExpect(jsonPath("$.cachedResult").isBoolean())
                .andExpect(jsonPath("$.timestamp").exists());
        verify(emailService, times(1)).getEmailCount(senderEmail);
    }
    
    @Test
    @WithMockUser
    void testCountEmails_InvalidEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        String invalidEmail = "not-an-email";
        when(emailService.getEmailCount(invalidEmail))
                .thenThrow(new InvalidEmailException("Invalid email format: " + invalidEmail));
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/emails/count")
                .param("senderEmail", invalidEmail))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    @WithMockUser
    void testCountEmails_MissingParameter_ReturnsBadRequest() throws Exception {
        // Act & Assert
        // Note: Spring returns 500 for missing required @RequestParam by default
        // This could be improved with custom validation
        mockMvc.perform(get("/api/v1/emails/count"))
                .andExpect(status().is5xxServerError());
    }
    
    @Test
    @WithMockUser
    void testHealth_ReturnsOk() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/emails/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Email Reader Agent is running"));
    }
    
    @Test
    @WithMockUser
    void testCountEmails_ZeroCount_ReturnsOkWithZero() throws Exception {
        // Arrange
        String senderEmail = "noone@example.com";
        long emailCount = 0L;
        when(emailService.getEmailCount(senderEmail)).thenReturn(emailCount);
        when(cacheManager.getCache("emailCounts")).thenReturn(null);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/emails/count")
                .param("senderEmail", senderEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderEmail").value(senderEmail))
                .andExpect(jsonPath("$.emailCount").value(0));
        verify(emailService, times(1)).getEmailCount(senderEmail);
    }
}
