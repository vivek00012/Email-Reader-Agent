package com.krysta.emailreader.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.krysta.emailreader.config.GmailConfig;
import com.krysta.emailreader.exception.GmailApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;

/**
 * Service class for Gmail API integration.
 * Handles OAuth 2.0 authentication and email counting operations.
 */
@Service
public class GmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(GmailService.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String USER_ID = "me";
    
    private final GmailConfig gmailConfig;
    private Gmail gmailClient;
    
    public GmailService(GmailConfig gmailConfig) {
        this.gmailConfig = gmailConfig;
    }
    
    /**
     * Creates and returns an authorized Credential object.
     * 
     * @param httpTransport The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
        // Load client secrets from credentials file
        InputStream in = GmailService.class.getResourceAsStream(gmailConfig.getCredentialsFile());
        if (in == null) {
            throw new GmailApiException(
                "Credentials file not found: " + gmailConfig.getCredentialsFile() + 
                ". Please follow the Gmail API setup instructions in README.md");
        }
        
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        
        // Build flow and trigger user authorization request
        File tokensDir = new File(gmailConfig.getTokensDirectory());
        if (!tokensDir.exists()) {
            tokensDir.mkdirs();
        }
        
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, gmailConfig.getScopes())
                .setDataStoreFactory(new FileDataStoreFactory(tokensDir))
                .setAccessType("offline")
                .build();
        
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();
        
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
    
    /**
     * Initializes and returns a Gmail service instance.
     * 
     * @return Gmail service instance
     * @throws GmailApiException if initialization fails
     */
    private Gmail getGmailClient() {
        if (gmailClient == null) {
            try {
                final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                Credential credential = getCredentials(httpTransport);
                
                gmailClient = new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                        .setApplicationName(gmailConfig.getApplicationName())
                        .build();
                
                logger.info("Gmail client initialized successfully");
            } catch (GeneralSecurityException | IOException e) {
                logger.error("Failed to initialize Gmail client", e);
                throw new GmailApiException("Failed to initialize Gmail client", e);
            }
        }
        return gmailClient;
    }
    
    /**
     * Counts the number of emails from a specific sender.
     * Handles pagination for large result sets.
     * 
     * @param senderEmail The email address of the sender
     * @return The count of emails from the sender
     * @throws GmailApiException if the Gmail API call fails
     */
    public long countEmailsFromSender(String senderEmail) {
        logger.debug("Counting emails from sender: {}", senderEmail);
        
        try {
            Gmail service = getGmailClient();
            String query = "from:" + senderEmail;
            long totalCount = 0;
            String pageToken = null;
            
            do {
                // List messages with query filter
                ListMessagesResponse response = service.users().messages().list(USER_ID)
                        .setQ(query)
                        .setMaxResults(500L)
                        .setPageToken(pageToken)
                        .execute();
                
                if (response.getMessages() != null) {
                    totalCount += response.getMessages().size();
                    logger.debug("Found {} messages in current page (total so far: {})", 
                               response.getMessages().size(), totalCount);
                }
                
                pageToken = response.getNextPageToken();
            } while (pageToken != null);
            
            logger.info("Total emails from {}: {}", senderEmail, totalCount);
            return totalCount;
            
        } catch (IOException e) {
            logger.error("Gmail API error while counting emails from {}", senderEmail, e);
            throw new GmailApiException("Failed to count emails from sender: " + senderEmail, e);
        }
    }
}
