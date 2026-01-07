package com.krysta.emailreader.security;

import com.google.api.client.util.store.AbstractDataStoreFactory;
import com.google.api.client.util.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Custom DataStoreFactory that encrypts OAuth tokens at rest using AES-256-GCM.
 */
public class EncryptedDataStoreFactory extends AbstractDataStoreFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(EncryptedDataStoreFactory.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    
    private final File dataDirectory;
    private final SecretKey encryptionKey;
    
    public EncryptedDataStoreFactory(File dataDirectory) throws IOException {
        this.dataDirectory = dataDirectory;
        
        if (!dataDirectory.exists()) {
            if (!dataDirectory.mkdirs()) {
                throw new IOException("Failed to create data directory: " + dataDirectory);
            }
        }
        
        // Load or generate encryption key
        this.encryptionKey = loadOrGenerateKey();
        logger.info("Initialized encrypted data store at: {}", dataDirectory.getAbsolutePath());
    }
    
    @Override
    protected <V extends Serializable> DataStore<V> createDataStore(String id) {
        return new EncryptedFileDataStore<>(this, dataDirectory, id, encryptionKey);
    }
    
    /**
     * Load existing encryption key or generate a new one.
     */
    private SecretKey loadOrGenerateKey() throws IOException {
        Path keyPath = Paths.get(dataDirectory.getAbsolutePath(), ".keystore");
        
        if (Files.exists(keyPath)) {
            try {
                byte[] keyBytes = Files.readAllBytes(keyPath);
                byte[] decodedKey = Base64.getDecoder().decode(keyBytes);
                logger.debug("Loaded existing encryption key");
                return new SecretKeySpec(decodedKey, ALGORITHM);
            } catch (Exception e) {
                logger.warn("Failed to load existing key, generating new one", e);
            }
        }
        
        // Generate new key
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_SIZE, new SecureRandom());
            SecretKey key = keyGen.generateKey();
            
            // Save key
            byte[] encodedKey = Base64.getEncoder().encode(key.getEncoded());
            Files.write(keyPath, encodedKey);
            
            // Set restrictive permissions (owner read/write only)
            keyPath.toFile().setReadable(false, false);
            keyPath.toFile().setReadable(true, true);
            keyPath.toFile().setWritable(false, false);
            keyPath.toFile().setWritable(true, true);
            
            logger.info("Generated new encryption key");
            return key;
            
        } catch (Exception e) {
            throw new IOException("Failed to generate encryption key", e);
        }
    }
    
    /**
     * Encrypt data using AES-256-GCM.
     */
    static byte[] encrypt(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        
        byte[] encrypted = cipher.doFinal(data);
        
        // Prepend IV to encrypted data
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        
        return result;
    }
    
    /**
     * Decrypt data using AES-256-GCM.
     */
    static byte[] decrypt(byte[] encryptedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        
        // Extract IV from encrypted data
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, iv.length);
        
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        
        // Decrypt the rest
        return cipher.doFinal(encryptedData, iv.length, encryptedData.length - iv.length);
    }
}
