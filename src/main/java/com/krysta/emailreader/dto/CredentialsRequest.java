package com.krysta.emailreader.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Gmail OAuth credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Gmail OAuth 2.0 credentials JSON")
public class CredentialsRequest {
    
    @Schema(
        description = "Complete Google OAuth credentials JSON as string",
        example = "{\"installed\":{\"client_id\":\"...\",\"project_id\":\"...\",\"auth_uri\":\"...\",\"token_uri\":\"...\",\"auth_provider_x509_cert_url\":\"...\",\"client_secret\":\"...\",\"redirect_uris\":[\"http://localhost\"]}}"
    )
    private String credentialsJson;
}
