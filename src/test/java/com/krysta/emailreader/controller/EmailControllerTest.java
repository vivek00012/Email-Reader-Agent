package com.krysta.emailreader.controller;

import com.krysta.emailreader.exception.InvalidEmailException;
import com.krysta.emailreader.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    
    @Test
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
    void testCountEmails_MissingParameter_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/emails/count"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testHealth_ReturnsOk() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/emails/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Email Reader Agent is running"));
    }
    
    @Test
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
