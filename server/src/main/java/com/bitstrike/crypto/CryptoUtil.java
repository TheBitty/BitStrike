package com.bitstrike.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;

/**
 * Utility class for encryption and decryption operations.
 * 
 * Note: In a real-world scenario, proper key management would be implemented.
 * For Phase 1, we're using a static key for simplicity.
 */
public class CryptoUtil {
    private static final Logger logger = LoggerFactory.getLogger(CryptoUtil.class);
    
    // In a real implementation, keys should never be hardcoded and should be securely managed
    // This is just for Phase 1 prototype purposes
    private static final byte[] KEY_BYTES = "ThisIsA32ByteKeyForAES256Encrypt".getBytes(StandardCharsets.UTF_8);
    private static final byte[] IV_BYTES = "RandomInitVector".getBytes(StandardCharsets.UTF_8);
    
    private final SecretKey secretKey;
    private final IvParameterSpec iv;
    
    static {
        // Register Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());
    }
    
    public CryptoUtil() {
        this.secretKey = new SecretKeySpec(KEY_BYTES, "AES");
        this.iv = new IvParameterSpec(IV_BYTES);
    }
    
    /**
     * Encrypts a string using AES-256.
     */
    public String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            logger.error("Encryption error", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Decrypts a Base64 encoded encrypted string using AES-256.
     */
    public String decrypt(String encryptedBase64) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Decryption error", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public boolean setCommandResult(String agentId, String commandId, String result, boolean success) {
        // In a real implementation, we would store command results
        // For simplicity in Phase 1, we'll just log them
        if (success) {
            logger.info("Command {} completed on agent {}: {}", commandId, agentId, result);
        } else {
            logger.error("Command {} failed on agent {}: {}", commandId, agentId, result);
        }
        return true;
    }
} 