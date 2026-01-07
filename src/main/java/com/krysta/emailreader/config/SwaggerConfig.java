package com.krysta.emailreader.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 3.0 configuration for API documentation.
 */
@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI emailReaderOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local Development Server");
        
        Contact contact = new Contact();
        contact.setName("Krysta Software");
        contact.setEmail("support@krysta.com");
        
        License license = new License();
        license.setName("MIT License");
        license.setUrl("https://opensource.org/licenses/MIT");
        
        Info info = new Info()
                .title("Email Reader Agent API")
                .version("1.0.0")
                .description("REST API service that integrates with Gmail to count emails from specific senders. " +
                             "This API allows you to query the number of emails received from any email address.\n\n" +
                             "**Authentication:** When you call the email count API for the first time, the Google OAuth consent screen will open automatically in your browser. " +
                             "Approve access once; tokens are stored locally for reuse.\n\n" +
                             "**Credentials:** You can provide Gmail OAuth credentials via the `/api/v1/emails/credentials` endpoint. " +
                             "Alternatively, place credentials.json in src/main/resources/. " +
                             "Credentials provided via API are stored in memory and take precedence over file-based credentials.")
                .contact(contact)
                .license(license);
        
        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
