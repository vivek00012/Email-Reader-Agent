package com.krysta.emailreader.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for email count queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing the count of emails from a specific sender")
public class EmailCountResponse {
    
    @JsonProperty("senderEmail")
    @Schema(description = "Email address of the sender", example = "superman@example.com")
    private String senderEmail;
    
    @JsonProperty("emailCount")
    @Schema(description = "Number of emails received from the sender", example = "10")
    private long emailCount;
    
    @JsonProperty("cachedResult")
    @Schema(description = "Indicates if the result was retrieved from cache", example = "false")
    private boolean cachedResult;
    
    @JsonProperty("timestamp")
    @Schema(description = "Timestamp when the response was generated")
    private LocalDateTime timestamp;
}
