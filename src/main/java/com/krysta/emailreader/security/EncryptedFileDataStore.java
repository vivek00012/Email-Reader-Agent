package com.krysta.emailreader.security;

import com.google.api.client.util.store.AbstractDataStore;
import com.google.api.client.util.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Set;

/**
 * Encrypted file-based implementation of DataStore for secure token storage.
 */
class EncryptedFileDataStore<V extends Serializable> extends AbstractDataStore<V> {
    
    private static final Logger logger = LoggerFactory.getLogger(EncryptedFileDataStore.class);
    
    private final File dataDirectory;
    private final SecretKey encryptionKey;
    
    EncryptedFileDataStore(EncryptedDataStoreFactory dataStoreFactory, 
                          File dataDirectory, 
                          String id,
                          SecretKey encryptionKey) {
        super(dataStoreFactory, id);
        this.dataDirectory = dataDirectory;
        this.encryptionKey = encryptionKey;
    }
    
    @Override
    public Set<String> keySet() throws IOException {
        File[] files = dataDirectory.listFiles((dir, name) -> !name.equals(".keystore"));
        Set<String> keys = new HashMap<String, V>().keySet();
        
        if (files != null) {
            for (File file : files) {
                keys.add(file.getName());
            }
        }
        
        return keys;
    }
    
    @Override
    public DataStore<V> set(String key, V value) throws IOException {
        File file = new File(dataDirectory, key);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            
            oos.writeObject(value);
            oos.flush();
            
            byte[] serialized = baos.toByteArray();
            byte[] encrypted = EncryptedDataStoreFactory.encrypt(serialized, encryptionKey);
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(encrypted);
            }
            
            logger.debug("Stored encrypted value for key: {}", key);
            
        } catch (Exception e) {
            throw new IOException("Failed to encrypt and store data", e);
        }
        
        return this;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public V get(String key) throws IOException {
        File file = new File(dataDirectory, key);
        
        if (!file.exists()) {
            return null;
        }
        
        try {
            byte[] encrypted = Files.readAllBytes(file.toPath());
            byte[] decrypted = EncryptedDataStoreFactory.decrypt(encrypted, encryptionKey);
            
            try (ByteArrayInputStream bais = new ByteArrayInputStream(decrypted);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                
                logger.debug("Retrieved and decrypted value for key: {}", key);
                return (V) ois.readObject();
            }
            
        } catch (Exception e) {
            throw new IOException("Failed to decrypt and retrieve data", e);
        }
    }
    
    @Override
    public DataStore<V> clear() throws IOException {
        File[] files = dataDirectory.listFiles((dir, name) -> !name.equals(".keystore"));
        
        if (files != null) {
            for (File file : files) {
                if (!file.delete()) {
                    logger.warn("Failed to delete file: {}", file.getName());
                }
            }
        }
        
        logger.info("Cleared all stored data");
        return this;
    }
    
    @Override
    public DataStore<V> delete(String key) throws IOException {
        File file = new File(dataDirectory, key);
        
        if (file.exists() && !file.delete()) {
            logger.warn("Failed to delete file for key: {}", key);
        }
        
        logger.debug("Deleted data for key: {}", key);
        return this;
    }
    
    @Override
    public java.util.Collection<V> values() throws IOException {
        // Load all values from stored files
        Set<String> keys = keySet();
        java.util.List<V> values = new java.util.ArrayList<>();
        
        for (String key : keys) {
            V value = get(key);
            if (value != null) {
                values.add(value);
            }
        }
        
        return values;
    }
}
