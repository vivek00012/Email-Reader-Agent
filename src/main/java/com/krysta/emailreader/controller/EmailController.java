package com.krysta.emailreader.controller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.krysta.emailreader.config.GmailConfig;
import com.krysta.emailreader.dto.EmailCountResponse;
import com.krysta.emailreader.dto.ErrorResponse;
import com.krysta.emailreader.service.AuditService;
import com.krysta.emailreader.service.CredentialsStorageService;
import com.krysta.emailreader.service.EmailService;
import com.krysta.emailreader.util.LogSanitizer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

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
    private final AuditService auditService;
    private final CredentialsStorageService credentialsStorageService;
    private final GmailConfig gmailConfig;
    
    public EmailController(
            EmailService emailService, 
            CacheManager cacheManager, 
            AuditService auditService,
            CredentialsStorageService credentialsStorageService,
            GmailConfig gmailConfig) {
        this.emailService = emailService;
        this.cacheManager = cacheManager;
        this.auditService = auditService;
        this.credentialsStorageService = credentialsStorageService;
        this.gmailConfig = gmailConfig;
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
            @RequestParam String senderEmail,
            HttpServletRequest request) {
        
        String clientIp = getClientIpAddress(request);
        logger.info("Received request to count emails from: {}", LogSanitizer.maskEmail(senderEmail));
        
        // Check if result is in cache before calling service
        boolean isCached = false;
        if (cacheManager != null) {
            var cache = cacheManager.getCache("emailCounts");
            if (cache != null) {
                isCached = cache.get(senderEmail) != null;
            }
        }
        
        // Log cache operation
        if (auditService != null) {
            auditService.logCacheOperation("GET", senderEmail, isCached);
        }
        
        // Get email count (will use cache if available)
        long count = emailService.getEmailCount(senderEmail);
        
        // Audit log successful API access
        if (auditService != null) {
            auditService.logApiAccess("/api/v1/emails/count", clientIp, senderEmail);
        }
        
        // Build response
        EmailCountResponse response = EmailCountResponse.builder()
                .senderEmail(senderEmail)
                .emailCount(count)
                .cachedResult(isCached)
                .timestamp(LocalDateTime.now())
                .build();
        
        logger.info("Returning count for {}: {} (cached: {})", LogSanitizer.maskEmail(senderEmail), count, isCached);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Extract client IP address from request, considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Store Gmail OAuth credentials via file upload.
     * Upload credentials.json file containing Gmail OAuth 2.0 credentials.
     * Credentials are stored in memory and used for subsequent API calls.
     */
    @PostMapping(value = "/credentials", consumes = "multipart/form-data")
    @Operation(
        summary = "Upload Gmail OAuth credentials file",
        description = "Upload a credentials.json file containing Gmail OAuth 2.0 credentials. " +
                     "The file should be in the standard Google OAuth credentials format. " +
                     "These credentials will be used for all subsequent Gmail API calls. " +
                     "Credentials are stored in memory and will be cleared when the application restarts."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Credentials stored successfully",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid credentials file or format",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<Map<String, String>> setCredentials(
            @Parameter(
                description = "credentials.json file containing Gmail OAuth 2.0 credentials",
                required = true
            )
            @RequestPart("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        logger.info("Received credentials file upload request from IP: {}", LogSanitizer.maskIpAddress(clientIp));
        
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Credentials file is required");
            }
            
            // Validate file name (optional but recommended)
            String fileName = file.getOriginalFilename();
            if (fileName != null && !fileName.toLowerCase().endsWith(".json")) {
                logger.warn("Uploaded file does not have .json extension: {}", fileName);
            }
            
            // Read file content
            String credentialsJson = new String(file.getBytes(), StandardCharsets.UTF_8);
            
            if (credentialsJson == null || credentialsJson.trim().isEmpty()) {
                throw new IllegalArgumentException("Credentials file is empty");
            }
            
            // Store credentials
            credentialsStorageService.storeCredentials(credentialsJson);
            
            // Invalidate cache when new credentials are uploaded
            // This ensures fresh data is fetched with the new credentials
            if (cacheManager != null) {
                var cache = cacheManager.getCache("emailCounts");
                if (cache != null) {
                    cache.clear();
                    logger.info("Cache invalidated after credentials update");
                }
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Credentials stored successfully");
            response.put("filename", fileName != null ? fileName : "unknown");
            response.put("size", String.valueOf(file.getSize()));
            response.put("timestamp", LocalDateTime.now().toString());
            
            // Audit log
            if (auditService != null) {
                auditService.logApiAccess("/api/v1/emails/credentials", clientIp, "credentials_upload");
            }
            
            logger.info("Successfully stored credentials from file: {} ({} bytes)", fileName, file.getSize());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid credentials file: {}", e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Invalid credentials file: " + e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (IOException e) {
            logger.error("Failed to read credentials file", e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to read credentials file: " + e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to store credentials", e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Invalid credentials format: " + e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * Clear stored credentials, delete OAuth tokens, and invalidate cache.
     */
    @DeleteMapping("/credentials")
    @Operation(
        summary = "Clear stored credentials",
        description = "Remove stored Gmail OAuth credentials from memory, delete OAuth tokens, and invalidate email count cache. " +
                     "After clearing, the application will fall back to file-based credentials and require new OAuth authorization. " +
                     "All cached email counts will be cleared to ensure fresh data."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Credentials cleared, tokens deleted, and cache invalidated successfully"
    )
    public ResponseEntity<Map<String, String>> clearCredentials(HttpServletRequest httpRequest) {
        String clientIp = getClientIpAddress(httpRequest);
        logger.info("Clearing credentials from IP: {}", LogSanitizer.maskIpAddress(clientIp));
        
        // Clear stored credentials
        credentialsStorageService.clearAll();
        
        // Delete OAuth token files
        deleteTokenFiles();
        
        // Invalidate cache when credentials are deleted
        // This ensures cached data doesn't persist with old credentials
        if (cacheManager != null) {
            var cache = cacheManager.getCache("emailCounts");
            if (cache != null) {
                cache.clear();
                logger.info("Cache invalidated after credentials deletion");
            }
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Credentials cleared, tokens deleted, and cache invalidated successfully");
        response.put("timestamp", LocalDateTime.now().toString());
        
        // Audit log
        if (auditService != null) {
            auditService.logApiAccess("/api/v1/emails/credentials", clientIp, "credentials_deleted");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Deletes OAuth token files from the tokens directory.
     */
    private void deleteTokenFiles() {
        try {
            File tokensDir = new File(gmailConfig.getTokensDirectory());
            if (!tokensDir.exists()) {
                logger.debug("Tokens directory does not exist: {}", tokensDir.getAbsolutePath());
                return;
            }
            
            // Delete the user token file
            File userTokenFile = new File(tokensDir, "user");
            if (userTokenFile.exists()) {
                try {
                    Files.delete(userTokenFile.toPath());
                    logger.info("Deleted OAuth token file: {}", userTokenFile.getAbsolutePath());
                } catch (IOException e) {
                    logger.warn("Failed to delete token file {}: {}", userTokenFile.getAbsolutePath(), e.getMessage());
                }
            }
            
            // Delete the keystore file if it exists
            File keystoreFile = new File(tokensDir, ".keystore");
            if (keystoreFile.exists()) {
                try {
                    Files.delete(keystoreFile.toPath());
                    logger.info("Deleted keystore file: {}", keystoreFile.getAbsolutePath());
                } catch (IOException e) {
                    logger.warn("Failed to delete keystore file {}: {}", keystoreFile.getAbsolutePath(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error deleting token files: {}", e.getMessage(), e);
            // Don't throw - allow credentials clearing to succeed even if token deletion fails
        }
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
