package com.krysta.emailreader.service;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Files;
import java.security.GeneralSecurityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.krysta.emailreader.config.GmailConfig;
import com.krysta.emailreader.exception.GmailApiException;
import com.krysta.emailreader.security.EncryptedDataStoreFactory;
import com.krysta.emailreader.util.LogSanitizer;

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
    private final CredentialsStorageService credentialsStorageService;
    
    public GmailService(GmailConfig gmailConfig, 
                        CredentialsStorageService credentialsStorageService) {
        this.gmailConfig = gmailConfig;
        this.credentialsStorageService = credentialsStorageService;
    }
    
    /**
     * Creates and returns an authorized Credential object.
     * Checks for in-memory credentials first, then falls back to file.
     */
    private Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
        GoogleAuthorizationCodeFlow flow = buildAuthorizationFlow(httpTransport);
        Credential credential = loadExistingCredential(flow);
        
        if (credential == null || credential.getAccessToken() == null || !refreshTokenIfNeeded(credential)) {
            credential = performOAuthAuthorization(flow);
        }
        
        return credential;
    }
    
    /**
     * Loads client secrets from storage or file.
     */
    private GoogleClientSecrets loadClientSecrets() throws IOException {
        String credentialsJson = credentialsStorageService.getCredentials();
        if (credentialsJson != null && !credentialsJson.isEmpty()) {
            logger.info("Using credentials from in-memory storage");
            return GoogleClientSecrets.load(JSON_FACTORY, new StringReader(credentialsJson));
        }
        
        logger.info("Using credentials from file: {}", gmailConfig.getCredentialsFile());
        InputStream in = GmailService.class.getResourceAsStream(gmailConfig.getCredentialsFile());
        if (in == null) {
            throw new GmailApiException(
                "Credentials not found. Please provide credentials via /api/v1/emails/credentials endpoint " +
                "or ensure credentials.json file exists at: " + gmailConfig.getCredentialsFile());
        }
        return GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
    }
    
    /**
     * Builds the Google Authorization Code Flow.
     */
    private GoogleAuthorizationCodeFlow buildAuthorizationFlow(final NetHttpTransport httpTransport) throws IOException {
        File tokensDir = new File(gmailConfig.getTokensDirectory());
        if (!tokensDir.exists()) {
            tokensDir.mkdirs();
        }
        
        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, loadClientSecrets(), gmailConfig.getScopes())
                .setDataStoreFactory(new EncryptedDataStoreFactory(tokensDir))
                .setAccessType("offline")
                .build();
    }
    
    /**
     * Loads existing credential, handling decryption failures.
     */
    private Credential loadExistingCredential(GoogleAuthorizationCodeFlow flow) {
        try {
            return flow.loadCredential("user");
        } catch (IOException e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("decrypt") || errorMsg.contains("ArrayIndexOutOfBoundsException"))) {
                logger.warn("Failed to decrypt token. Will trigger new OAuth flow: {}", e.getMessage());
                deleteCorruptedTokenFile();
                return null;
            }
            throw new GmailApiException("Failed to load credential", e);
        }
    }
    
    /**
     * Deletes corrupted token file.
     */
    private void deleteCorruptedTokenFile() {
        try {
            File tokenFile = new File(gmailConfig.getTokensDirectory(), "user");
            if (tokenFile.exists()) {
                Files.delete(tokenFile.toPath());
                logger.info("Deleted corrupted token file");
            }
        } catch (Exception e) {
            logger.debug("Could not delete corrupted token file: {}", e.getMessage());
        }
    }
    
    /**
     * Refreshes token if expired or about to expire. Returns false if OAuth is needed.
     */
    private boolean refreshTokenIfNeeded(Credential credential) {
        Long expiresIn = credential.getExpiresInSeconds();
        if (expiresIn != null && expiresIn > 300) {
            logger.debug("Token is valid (expires in {} seconds)", expiresIn);
            return true;
        }
        
        if (credential.getRefreshToken() == null) {
            logger.warn("No refresh token available");
            return false;
        }
        
        try {
            logger.info("Refreshing access token (expiresIn: {} seconds)...", expiresIn);
            credential.refreshToken();
            logger.info("Token refreshed successfully. New expiration: {} seconds", 
                       credential.getExpiresInSeconds());
            return true;
        } catch (Exception e) {
            logger.warn("Failed to refresh token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Performs OAuth authorization flow.
     */
    private Credential performOAuthAuthorization(GoogleAuthorizationCodeFlow flow) throws IOException {
        logger.info("Starting OAuth authorization flow - browser will open shortly...");
        
        LocalServerReceiver receiver = null;
        int port = gmailConfig.getOauthCallbackPort();
        
        // Try to find available port (binding happens inside app.authorize())
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                receiver = new LocalServerReceiver.Builder().setPort(port).build();
                
                AuthorizationCodeInstalledApp app = new AuthorizationCodeInstalledApp(
                        flow, receiver, this::openBrowser);
                
                logger.info("Waiting for OAuth authorization in browser (port {})...", port);
                Credential credential = app.authorize("user");
                
                if (credential == null || credential.getAccessToken() == null) {
                    throw new GmailApiException("OAuth authorization failed. Please ensure:\n" +
                            "1. You completed OAuth in the browser\n" +
                            "2. Redirect URI in Google Cloud Console matches the callback port");
                }
                
                logger.info("OAuth authorization completed. Token expires in: {} seconds", 
                           credential.getExpiresInSeconds());
                
                // Stop receiver after successful authorization
                try {
                    receiver.stop();
                } catch (Exception e) {
                    logger.debug("Error stopping receiver: {}", e.getMessage());
                }
                
                return credential;
                
            } catch (java.net.BindException e) {
                handlePortConflict(e, attempt, port, receiver);
                port++;
                receiver = null;
            } catch (com.google.api.client.auth.oauth2.TokenResponseException e) {
                stopReceiver(receiver);
                throw new GmailApiException("OAuth token exchange failed: " + e.getMessage(), e);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Address already in use")) {
                    handlePortConflict(e, attempt, port, receiver);
                    port++;
                    receiver = null;
                    continue; // Retry with next port
                }
                stopReceiver(receiver);
                throw new GmailApiException("OAuth authorization failed: " + errorMsg, e);
            }
        }
        
        throw new GmailApiException("Failed to create OAuth callback receiver");
    }
    
    /**
     * Handles port conflict by stopping receiver and either retrying or throwing exception.
     */
    private void handlePortConflict(Exception e, int attempt, int port, LocalServerReceiver receiver) {
        stopReceiver(receiver);
        if (attempt < 2) {
            logger.warn("Port {} is in use, trying port {}...", port, port + 1);
        } else {
            throw new GmailApiException("OAuth callback ports " + 
                    gmailConfig.getOauthCallbackPort() + "-" + (port + 1) + 
                    " are in use. Please stop other applications or change port in application.yml");
        }
    }
    
    /**
     * Stops the receiver if not null.
     */
    private void stopReceiver(LocalServerReceiver receiver) {
        if (receiver != null) {
            try {
                receiver.stop();
            } catch (Exception e) {
                logger.debug("Error stopping receiver: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Opens browser with OAuth URL.
     */
    private void openBrowser(String url) {
        try {
            logger.info("Opening browser for OAuth authorization: {}", url);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
            
            String os = System.getProperty("os.name").toLowerCase();
            Runtime runtime = Runtime.getRuntime();
            if (os.contains("mac")) {
                runtime.exec(new String[]{"open", url});
            } else if (os.contains("nix") || os.contains("nux")) {
                runtime.exec(new String[]{"xdg-open", url});
            } else if (os.contains("win")) {
                runtime.exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            }
        } catch (Exception e) {
            logger.error("Failed to open browser. Please open manually: {}", url, e);
        }
    }
    
    /**
     * Initializes and returns a Gmail service instance.
     */
    private Gmail getGmailClient() {
        try {
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = getCredentials(httpTransport);
            
            return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(gmailConfig.getApplicationName())
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            logger.error("Failed to initialize Gmail client", e);
            throw new GmailApiException("Failed to initialize Gmail client: " + e.getMessage(), e);
        }
    }
    
    /**
     * Counts the number of emails from a specific sender.
     * Handles pagination for large result sets.
     */
    public long countEmailsFromSender(String senderEmail) {
        logger.debug("Counting emails from sender: {}", LogSanitizer.maskEmail(senderEmail));
        
        try {
            Gmail service = getGmailClient();
            long totalCount = 0;
            String pageToken = null;
            
            do {
                if (Thread.currentThread().isInterrupted()) {
                    throw new GmailApiException("Request was cancelled");
                }
                
                ListMessagesResponse response = service.users().messages().list(USER_ID)
                        .setQ("from:" + senderEmail)
                        .setMaxResults(500L)
                        .setPageToken(pageToken)
                        .execute();
                
                if (response.getMessages() != null) {
                    totalCount += response.getMessages().size();
                }
                
                pageToken = response.getNextPageToken();
            } while (pageToken != null);
            
            logger.info("Total emails from {}: {}", LogSanitizer.maskEmail(senderEmail), totalCount);
            return totalCount;
            
        } catch (IOException e) {
            if (isClientDisconnection(e)) {
                throw new GmailApiException("Request was cancelled");
            }
            logger.error("Gmail API error while counting emails from {}", LogSanitizer.maskEmail(senderEmail), e);
            throw new GmailApiException("Failed to count emails from sender", e);
        }
    }
    
    /**
     * Checks if IOException is due to client disconnection.
     */
    private boolean isClientDisconnection(IOException e) {
        String message = e.getMessage();
        return message != null && (
            message.contains("Broken pipe") ||
            message.contains("Connection reset") ||
            message.contains("Connection closed")
        );
    }
}
