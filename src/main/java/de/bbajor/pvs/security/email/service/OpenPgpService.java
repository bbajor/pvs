package de.bbajor.pvs.security.email.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.pgpainless.PGPainless;
import org.pgpainless.algorithm.SymmetricKeyAlgorithm;
import org.pgpainless.encryption_signing.EncryptionOptions;
import org.pgpainless.encryption_signing.EncryptionStream;
import org.pgpainless.encryption_signing.ProducerOptions;
import org.pgpainless.encryption_signing.SigningOptions;
import org.pgpainless.key.protection.SecretKeyRingProtector;
import org.pgpainless.util.Passphrase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for OpenPGP encryption and key management using PGPainless.
 * PGPainless provides a modern, high-level API for OpenPGP operations.
 */
@Service
public class OpenPgpService {

    private static final Logger log = LoggerFactory.getLogger(OpenPgpService.class);

    /**
     * Validates and parses an OpenPGP public key.
     * 
     * @param armoredKey The ASCII-armored public key
     * @return The parsed PGP public key ring
     * @throws PGPException If the key is invalid
     * @throws IOException If there's an error reading the key
     */
    public PGPPublicKeyRing parsePublicKey(String armoredKey) throws PGPException, IOException {
        try {
            return PGPainless.readKeyRing()
                    .publicKeyRing(armoredKey);
        } catch (Exception e) {
            throw new PGPException("Failed to parse public key", e);
        }
    }

    /**
     * Extracts the key ID from a public key.
     * 
     * @param armoredKey The ASCII-armored public key
     * @return The key ID as a hex string (16 characters)
     * @throws PGPException If the key is invalid
     * @throws IOException If there's an error reading the key
     */
    public String extractKeyId(String armoredKey) throws PGPException, IOException {
        try {
            PGPPublicKeyRing keyRing = parsePublicKey(armoredKey);
            long keyId = keyRing.getPublicKey().getKeyID();
            return Long.toHexString(keyId).toUpperCase();
        } catch (Exception e) {
            throw new PGPException("Failed to extract key ID", e);
        }
    }

    /**
     * Extracts the fingerprint from a public key.
     * 
     * @param armoredKey The ASCII-armored public key
     * @return The fingerprint as a hex string (40 characters)
     * @throws PGPException If the key is invalid
     * @throws IOException If there's an error reading the key
     */
    public String extractFingerprint(String armoredKey) throws PGPException, IOException {
        try {
            PGPPublicKeyRing keyRing = parsePublicKey(armoredKey);
            byte[] fingerprint = keyRing.getPublicKey().getFingerprint();
            return formatFingerprint(fingerprint);
        } catch (Exception e) {
            throw new PGPException("Failed to extract fingerprint", e);
        }
    }

    /**
     * Extracts the key ID from a private key.
     * 
     * @param armoredPrivateKey The ASCII-armored private key
     * @return The key ID as a hex string (16 characters)
     * @throws PGPException If the key is invalid
     * @throws IOException If there's an error reading the key
     */
    private String extractKeyIdFromPrivateKey(String armoredPrivateKey) throws PGPException, IOException {
        try {
            PGPSecretKeyRing secretKeyRing = parseSecretKey(armoredPrivateKey);
            if (secretKeyRing == null || secretKeyRing.getPublicKey() == null) {
                throw new PGPException("Invalid private key: no public key found");
            }
            long keyId = secretKeyRing.getPublicKey().getKeyID();
            return Long.toHexString(keyId).toUpperCase();
        } catch (Exception e) {
            throw new PGPException("Failed to extract key ID from private key", e);
        }
    }

    /**
     * Encrypts and signs a message using OpenPGP.
     * Uses pgpainless to combine both operations in one step for proper PGP structure.
     * 
     * @param message The message to encrypt and sign
     * @param armoredPublicKey The ASCII-armored public key of the recipient (for encryption)
     * @param armoredPrivateKey The ASCII-armored private key of the sender (for signing)
     * @param privateKeyPassphrase The passphrase for the private key (can be null if not password-protected)
     * @return The encrypted and signed message as ASCII-armored string
     * @throws PGPException If encryption or signing fails
     * @throws IOException If there's an error during encryption or signing
     */
    public String encryptAndSignMessage(String message, String armoredPublicKey, 
            String armoredPrivateKey, String privateKeyPassphrase) throws PGPException, IOException {
        try {
            // Validate public key (for encryption)
            PGPPublicKeyRing recipientKeyRing = PGPainless.readKeyRing()
                    .publicKeyRing(armoredPublicKey);
            
            if (recipientKeyRing == null || recipientKeyRing.getPublicKey() == null) {
                throw new PGPException("Invalid public key: no encryption key found");
            }
            
            // Encrypt and sign: Standard PGP procedure is sign-then-encrypt
            // This means the signature is inside the encrypted message
            // Thunderbird/Enigmail will decrypt first, then verify the signature
            // Note: The signature will be verified AFTER decryption, which is the standard PGP flow
            // We do this in two steps to ensure proper PGP structure
            log.debug("Signing message before encryption...");
            String signed = signMessage(message, armoredPrivateKey, privateKeyPassphrase);
            log.debug("Message signed successfully, length: {} chars", signed.length());
            
            log.debug("Encrypting signed message...");
            String encrypted = encryptMessage(signed, armoredPublicKey);
            
            // Extract key ID for logging
            try {
                String senderKeyId = extractKeyIdFromPrivateKey(armoredPrivateKey);
                log.info("Email encrypted and signed successfully (sign-then-encrypt). " +
                        "Message length: {} chars. Signing key ID: {}. " +
                        "Note: Recipient needs sender's public key in their keyring to verify signature.",
                        encrypted.length(), senderKeyId);
            } catch (Exception e) {
                log.debug("Could not extract key ID for logging", e);
                log.info("Email encrypted and signed successfully (sign-then-encrypt). " +
                        "Message length: {} chars. " +
                        "Note: Recipient needs sender's public key in their keyring to verify signature.",
                        encrypted.length());
            }
            
            return encrypted;
        } catch (Exception e) {
            log.error("Failed to encrypt and sign message with pgpainless", e);
            if (e instanceof PGPException) {
                throw e;
            }
            throw new PGPException("Failed to encrypt and sign message: " + e.getMessage(), e);
        }
    }

    /**
     * Signs a message using an OpenPGP private key.
     * 
     * @param message The message to sign
     * @param armoredPrivateKey The ASCII-armored private key
     * @param passphrase The passphrase for the private key (can be null if not password-protected)
     * @return The signed message as ASCII-armored string
     * @throws PGPException If signing fails
     * @throws IOException If there's an error during signing
     */
    public String signMessage(String message, String armoredPrivateKey, String passphrase) 
            throws PGPException, IOException {
        try {
            // Parse secret key ring
            PGPSecretKeyRing secretKeyRing = PGPainless.readKeyRing()
                    .secretKeyRing(armoredPrivateKey);
            
            // Prepare protector for private key
            SecretKeyRingProtector protector;
            if (passphrase != null && !passphrase.isEmpty()) {
                protector = SecretKeyRingProtector.unlockAnyKeyWith(
                        Passphrase.fromPassword(passphrase));
            } else {
                protector = SecretKeyRingProtector.unlockAnyKeyWith(Passphrase.emptyPassphrase());
            }
            
            // Sign the message
            SigningOptions signingOptions = SigningOptions.get()
                    .addSignature(protector, secretKeyRing);
            
            ByteArrayOutputStream signedOut = new ByteArrayOutputStream();
            try (EncryptionStream signingStream = PGPainless.encryptAndOrSign()
                    .onOutputStream(signedOut)
                    .withOptions(ProducerOptions.sign(signingOptions))) {
                signingStream.write(message.getBytes(StandardCharsets.UTF_8));
            }
            
            String signed = signedOut.toString(StandardCharsets.UTF_8);
            return normalizeArmoredMessage(signed);
        } catch (Exception e) {
            if (e instanceof PGPException) {
                throw e;
            }
            throw new PGPException("Failed to sign message", e);
        }
    }

    /**
     * Encrypts a message using an OpenPGP public key.
     * 
     * @param message The message to encrypt
     * @param armoredPublicKey The ASCII-armored public key
     * @return The encrypted message as ASCII-armored string
     * @throws PGPException If encryption fails
     * @throws IOException If there's an error during encryption
     */
    public String encryptMessage(String message, String armoredPublicKey) throws PGPException, IOException {
        try {
            // Parse public key ring
            PGPPublicKeyRing recipientKeyRing = PGPainless.readKeyRing()
                    .publicKeyRing(armoredPublicKey);
            
            // Validate that we have a valid encryption key
            if (recipientKeyRing == null || recipientKeyRing.getPublicKey() == null) {
                throw new PGPException("Invalid public key: no encryption key found");
            }
            
            // Log key information for debugging
            long keyId = recipientKeyRing.getPublicKey().getKeyID();
            log.debug("Encrypting with public key ID: {}", Long.toHexString(keyId).toUpperCase());
            
            // Prepare encryption options
            EncryptionOptions encryptionOptions = EncryptionOptions.encryptCommunications()
                    .addRecipient(recipientKeyRing)
                    .overrideEncryptionAlgorithm(SymmetricKeyAlgorithm.AES_256);
            
            // Encrypt the message
            ByteArrayOutputStream encryptedOut = new ByteArrayOutputStream();
            try (EncryptionStream encryptionStream = PGPainless.encryptAndOrSign()
                    .onOutputStream(encryptedOut)
                    .withOptions(ProducerOptions.encrypt(encryptionOptions))) {
                encryptionStream.write(message.getBytes(StandardCharsets.UTF_8));
            }
            
            String encrypted = encryptedOut.toString(StandardCharsets.UTF_8);
            String normalized = normalizeArmoredMessage(encrypted);
            
            log.debug("Encryption successful, message length: {} chars", normalized.length());
            return normalized;
        } catch (Exception e) {
            log.error("Failed to encrypt message with pgpainless", e);
            if (e instanceof PGPException) {
                throw e;
            }
            throw new PGPException("Failed to encrypt message: " + e.getMessage(), e);
        }
    }

    /**
     * Validates that a public key is properly formatted.
     * 
     * @param armoredKey The ASCII-armored public key
     * @return true if the key is valid, false otherwise
     */
    public boolean isValidPublicKey(String armoredKey) {
        try {
            parsePublicKey(armoredKey);
            return true;
        } catch (Exception e) {
            log.debug("Invalid OpenPGP public key", e);
            return false;
        }
    }

    /**
     * Parses a secret key (private key) from ASCII-armored format.
     * 
     * @param armoredKey The ASCII-armored secret key
     * @return The parsed PGP secret key ring
     * @throws PGPException If the key is invalid
     * @throws IOException If there's an error reading the key
     */
    public PGPSecretKeyRing parseSecretKey(String armoredKey) throws PGPException, IOException {
        try {
            return PGPainless.readKeyRing()
                    .secretKeyRing(armoredKey);
        } catch (Exception e) {
            throw new PGPException("Failed to parse secret key", e);
        }
    }

    /**
     * Extracts the public key from a private key ring.
     * This is useful for creating Autocrypt headers.
     * 
     * @param armoredPrivateKey The ASCII-armored private key
     * @return The ASCII-armored public key
     * @throws PGPException If the key is invalid
     * @throws IOException If there's an error reading the key
     */
    public String extractPublicKeyFromPrivateKey(String armoredPrivateKey) throws PGPException, IOException {
        try {
            PGPSecretKeyRing secretKeyRing = parseSecretKey(armoredPrivateKey);
            if (secretKeyRing == null) {
                throw new PGPException("Failed to parse secret key ring");
            }
            
            // Extract public key ring from secret key ring
            // PGPSecretKeyRing contains public keys, we can extract them
            java.util.Iterator<org.bouncycastle.openpgp.PGPPublicKey> publicKeys = secretKeyRing.getPublicKeys();
            java.util.List<org.bouncycastle.openpgp.PGPPublicKey> publicKeyList = new java.util.ArrayList<>();
            while (publicKeys.hasNext()) {
                publicKeyList.add(publicKeys.next());
            }
            
            // Create a new public key ring from the extracted public keys
            PGPPublicKeyRing publicKeyRing = new PGPPublicKeyRing(publicKeyList);
            
            // Convert to ASCII-armored format using BouncyCastle directly
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (org.bouncycastle.bcpg.ArmoredOutputStream armoredOut = 
                    new org.bouncycastle.bcpg.ArmoredOutputStream(baos)) {
                publicKeyRing.encode(armoredOut);
            }
            
            String publicKey = baos.toString(StandardCharsets.UTF_8);
            return normalizeArmoredMessage(publicKey);
        } catch (Exception e) {
            if (e instanceof PGPException) {
                throw e;
            }
            throw new PGPException("Failed to extract public key from private key: " + e.getMessage(), e);
        }
    }

    /**
     * Formats a fingerprint byte array as a hex string.
     * 
     * @param fingerprint The fingerprint bytes
     * @return The formatted fingerprint string
     */
    private String formatFingerprint(byte[] fingerprint) {
        StringBuilder sb = new StringBuilder();
        for (byte b : fingerprint) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * Normalizes an ASCII-armored PGP message for better compatibility with email clients.
     * Removes version lines, normalizes line endings, and ensures proper formatting.
     * 
     * @param armoredMessage The ASCII-armored message
     * @return The normalized message
     */
    private String normalizeArmoredMessage(String armoredMessage) {
        if (armoredMessage == null) {
            return null;
        }
        
        String normalized = armoredMessage;
        
        // Remove version lines that can cause issues with some PGP clients
        // Remove BouncyCastle version lines
        normalized = normalized.replaceAll("(?m)^Version: BCPG v@RELEASE_NAME@\\r?\\n", "");
        normalized = normalized.replaceAll("(?m)^Version: BCPG v[^\\r\\n]+\\r?\\n", "");
        // Remove PGPainless version lines (critical for compatibility)
        normalized = normalized.replaceAll("(?m)^Version: PGPainless\\r?\\n", "");
        normalized = normalized.replaceAll("(?m)^Version: PGPainless[^\\r\\n]*\\r?\\n", "");
        
        // Normalize line endings to LF (Unix-style) for better compatibility
        normalized = normalized.replace("\r\n", "\n");
        normalized = normalized.replace("\r", "\n");
        
        // Remove leading/trailing whitespace
        normalized = normalized.trim();
        
        // Ensure no blank line after -----BEGIN PGP MESSAGE-----
        normalized = normalized.replaceAll("(?m)^-----BEGIN PGP MESSAGE-----\\n\\n", "-----BEGIN PGP MESSAGE-----\n");
        
        // Ensure no blank line before -----END PGP MESSAGE-----
        normalized = normalized.replaceAll("(?m)\\n\\n-----END PGP MESSAGE-----$", "\n-----END PGP MESSAGE-----");
        
        // Remove trailing newlines
        normalized = normalized.replaceAll("\\n+$", "");
        
        return normalized;
    }
}
