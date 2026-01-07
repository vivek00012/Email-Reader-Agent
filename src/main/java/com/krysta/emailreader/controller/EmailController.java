package com.krysta.emailreader.controller;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.krysta.emailreader.dto.EmailCountResponse;
import com.krysta.emailreader.dto.ErrorResponse;
import com.krysta.emailreader.service.EmailService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller for email counting operations.
 * Provides endpoints to query email counts from specific senders.
 */
@RestController
@RequestMapping("/api/v1/emails")
@Tag(name = "Email Operations", description = "APIs for counting emails from specific senders")
public class EmailController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);
    
    private final EmailService emailService;
    private final CacheManager cacheManager;
    
    public EmailController(EmailService emailService, CacheManager cacheManager) {
        this.emailService = emailService;
        this.cacheManager = cacheManager;
    }
    
    /**
     * Count emails from a specific sender.
     * 
     * @param senderEmail The email address of the sender
     * @return EmailCountResponse with the count and metadata
     */
    @GetMapping("/count")
    @Operation(
        summary = "Count emails from sender",
        description = "Returns the total number of emails received from a specific sender. " +
                     "Results are cached for 5 minutes to reduce Gmail API calls."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved email count",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = EmailCountResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid email format",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Gmail authentication failed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Gmail API rate limit exceeded",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<EmailCountResponse> countEmails(
            @Parameter(
                description = "Email address of the sender to count emails from",
                example = "superman@example.com",
                required = true
            )
            @RequestParam String senderEmail) {
        
        logger.info("Received request to count emails from: {}", senderEmail);
        
        // Check if result is in cache before calling service
        boolean isCached = cacheManager.getCache("emailCounts") != null &&
                          cacheManager.getCache("emailCounts").get(senderEmail) != null;
        
        // Get email count (will use cache if available)
        long count = emailService.getEmailCount(senderEmail);
        
        // Build response
        EmailCountResponse response = EmailCountResponse.builder()
                .senderEmail(senderEmail)
                .emailCount(count)
                .cachedResult(isCached)
                .timestamp(LocalDateTime.now())
                .build();
        
        logger.info("Returning count for {}: {} (cached: {})", senderEmail, count, isCached);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint to verify service is running.
     * 
     * @return Simple health status message
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Simple endpoint to verify the email service is running"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Service is healthy",
        content = @Content(mediaType = "text/plain")
    )
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Email Reader Agent is running");
    }
}
