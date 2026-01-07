package com.krysta.emailreader.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard error response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response containing details about the failure")
public class ErrorResponse {
    
    @JsonProperty("status")
    @Schema(description = "HTTP status code", example = "400")
    private int status;
    
    @JsonProperty("message")
    @Schema(description = "Error message describing what went wrong", 
            example = "Invalid email format")
    private String message;
    
    @JsonProperty("timestamp")
    @Schema(description = "Timestamp when the error occurred")
    private LocalDateTime timestamp;
    
    @JsonProperty("path")
    @Schema(description = "API path where the error occurred", 
            example = "/api/v1/emails/count")
    private String path;
}
