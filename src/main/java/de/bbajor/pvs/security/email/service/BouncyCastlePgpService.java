package de.bbajor.pvs.security.email.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Alternative PGP service using BouncyCastle directly instead of PGPainless.
 * This implementation provides better compatibility with Thunderbird and other email clients.
 */
@Service
public class BouncyCastlePgpService {

    private static final Logger log = LoggerFactory.getLogger(BouncyCastlePgpService.class);

    /**
     * Encrypts and signs a message using BouncyCastle directly.
     * This creates a proper sign-then-encrypt structure that Thunderbird can verify.
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
            // Parse keys
            PGPPublicKeyRing recipientKeyRing = parsePublicKeyRing(armoredPublicKey);
            PGPSecretKeyRing senderKeyRing = parseSecretKeyRing(armoredPrivateKey);
            
            // Get encryption key from recipient
            PGPPublicKey encryptionKey = recipientKeyRing.getPublicKey();
            if (encryptionKey == null || !encryptionKey.isEncryptionKey()) {
                throw new PGPException("No encryption key found in recipient key ring");
            }
            
            // Get signing key from sender
            PGPSecretKey signingKey = null;
            Iterator<PGPSecretKey> secretKeys = senderKeyRing.getSecretKeys();
            while (secretKeys.hasNext()) {
                PGPSecretKey key = secretKeys.next();
                if (key.isSigningKey()) {
                    signingKey = key;
                    break;
                }
            }
            
            if (signingKey == null) {
                throw new PGPException("No signing key found in sender key ring");
            }
            
            // Extract private key for signing
            BcPBESecretKeyDecryptorBuilder decryptorBuilder = new BcPBESecretKeyDecryptorBuilder(
                    new BcPGPDigestCalculatorProvider());
            var privateKey = privateKeyPassphrase != null && !privateKeyPassphrase.isEmpty()
                    ? signingKey.extractPrivateKey(decryptorBuilder.build(privateKeyPassphrase.toCharArray()))
                    : signingKey.extractPrivateKey(decryptorBuilder.build(new char[0]));
            
            // Create output stream
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            ArmoredOutputStream armoredOut = new ArmoredOutputStream(bOut);
            
            // Create compressed data generator
            PGPCompressedDataGenerator compressedDataGenerator = new PGPCompressedDataGenerator(
                    CompressionAlgorithmTags.ZIP);
            
            // Create encrypted data generator
            PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(
                    new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
                            .setWithIntegrityPacket(true)
                            .setSecureRandom(new java.security.SecureRandom()));
            encryptedDataGenerator.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(encryptionKey));
            
            // Create literal data generator
            PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
            
            // Create signature generator
            PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(
                    new BcPGPContentSignerBuilder(signingKey.getPublicKey().getAlgorithm(),
                            HashAlgorithmTags.SHA256));
            
            // Add signature subpackets for better Thunderbird compatibility
            PGPSignatureSubpacketGenerator signatureSubpacketGenerator = new PGPSignatureSubpacketGenerator();
            signatureSubpacketGenerator.setIssuerFingerprint(false, signingKey.getPublicKey());
            signatureSubpacketGenerator.setIssuerKeyID(false, signingKey.getKeyID());
            signatureSubpacketGenerator.setSignatureCreationTime(false, new Date());
            signatureGenerator.setHashedSubpackets(signatureSubpacketGenerator.generate());
            signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);
            
            // Start encryption
            java.io.OutputStream encryptedStream = encryptedDataGenerator.open(armoredOut, new byte[4096]);
            
            // Start compression
            java.io.OutputStream compressedStream = compressedDataGenerator.open(encryptedStream);
            
            // Write one-pass signature
            PGPOnePassSignature onePassSignature = signatureGenerator.generateOnePassVersion(false);
            onePassSignature.encode(compressedStream);
            
            // Write literal data and sign
            // Use UTF8 format for better Thunderbird compatibility (some versions prefer UTF8 over BINARY)
            java.io.OutputStream literalStream = literalDataGenerator.open(compressedStream, PGPLiteralData.UTF8,
                    "message.txt", new Date(), new byte[4096]);
            
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            literalStream.write(messageBytes);
            signatureGenerator.update(messageBytes);
            literalStream.close();
            
            // Write signature
            PGPSignature signature = signatureGenerator.generate();
            signature.encode(compressedStream);
            compressedStream.close();
            encryptedStream.close();
            armoredOut.close();
            
            String result = bOut.toString(StandardCharsets.UTF_8);
            
            log.info("Message encrypted and signed using BouncyCastle (sign-then-encrypt), length: {} chars", 
                    result.length());
            
            return normalizeArmoredMessage(result);
            
        } catch (Exception e) {
            log.error("Failed to encrypt and sign message with BouncyCastle", e);
            if (e instanceof PGPException) {
                throw e;
            }
            throw new PGPException("Failed to encrypt and sign message: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses a public key ring from ASCII-armored format.
     */
    private PGPPublicKeyRing parsePublicKeyRing(String armoredKey) throws PGPException, IOException {
        try (java.io.InputStream in = PGPUtil.getDecoderStream(new java.io.ByteArrayInputStream(
                armoredKey.getBytes(StandardCharsets.UTF_8)))) {
            JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(in);
            Object obj = pgpFact.nextObject();
            if (obj instanceof PGPPublicKeyRing) {
                return (PGPPublicKeyRing) obj;
            }
            throw new PGPException("Invalid public key format");
        }
    }
    
    /**
     * Parses a secret key ring from ASCII-armored format.
     */
    private PGPSecretKeyRing parseSecretKeyRing(String armoredKey) throws PGPException, IOException {
        try (java.io.InputStream in = PGPUtil.getDecoderStream(new java.io.ByteArrayInputStream(
                armoredKey.getBytes(StandardCharsets.UTF_8)))) {
            JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(in);
            Object obj = pgpFact.nextObject();
            if (obj instanceof PGPSecretKeyRing) {
                return (PGPSecretKeyRing) obj;
            }
            throw new PGPException("Invalid secret key format");
        }
    }
    
    /**
     * Normalizes an ASCII-armored PGP message for better compatibility.
     */
    private String normalizeArmoredMessage(String armoredMessage) {
        if (armoredMessage == null) {
            return null;
        }
        
        String normalized = armoredMessage;
        
        // Normalize line endings to LF
        normalized = normalized.replace("\r\n", "\n");
        normalized = normalized.replace("\r", "\n");
        
        // Remove trailing whitespace
        normalized = normalized.trim();
        
        // Ensure no blank line after -----BEGIN PGP MESSAGE-----
        normalized = normalized.replaceAll("(?m)^-----BEGIN PGP MESSAGE-----\\n\\n", 
                "-----BEGIN PGP MESSAGE-----\n");
        
        // Ensure no blank line before -----END PGP MESSAGE-----
        normalized = normalized.replaceAll("(?m)\\n\\n-----END PGP MESSAGE-----$", 
                "\n-----END PGP MESSAGE-----");
        
        // Remove trailing newlines
        normalized = normalized.replaceAll("\\n+$", "");
        
        return normalized;
    }
}

