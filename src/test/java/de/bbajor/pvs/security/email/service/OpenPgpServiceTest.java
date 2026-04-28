package de.bbajor.pvs.security.email.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.junit.jupiter.api.Test;
import org.pgpainless.PGPainless;
import org.pgpainless.decryption_verification.ConsumerOptions;
import org.pgpainless.decryption_verification.DecryptionStream;
import org.pgpainless.decryption_verification.OpenPgpMetadata;
import org.pgpainless.key.protection.SecretKeyRingProtector;
import org.pgpainless.util.Passphrase;

class OpenPgpServiceTest {

    private final OpenPgpService openPgpService = new OpenPgpService();

    @Test
    void encryptAndSignMessage_isDecryptableAndVerified() throws Exception {
        // given
        String userId = "Sender <sender@example.com>";
        String passphrase = "SehrGeheim123!";

        Passphrase passphraseObj = Passphrase.fromPassword(passphrase);
        PGPSecretKeyRing secretKeyRing = PGPainless.generateKeyRing()
                .modernKeyRing(userId, passphraseObj);
        PGPPublicKeyRing publicKeyRing = PGPainless.extractCertificate(secretKeyRing);

        String armoredPrivateKey = armorSecretKeyRing(secretKeyRing);
        String armoredPublicKey = armorPublicKeyRing(publicKeyRing);

        String message = "Hallo Recovery!";

        // when
        String encrypted = openPgpService.encryptAndSignMessage(
                message, armoredPublicKey, armoredPrivateKey, passphrase);

        // then
        ByteArrayOutputStream decryptedOut = new ByteArrayOutputStream();
        SecretKeyRingProtector protector = SecretKeyRingProtector.unlockAnyKeyWith(
                Passphrase.fromPassword(passphrase));

        ConsumerOptions consumerOptions = ConsumerOptions.get()
                .addDecryptionKey(secretKeyRing, protector)
                .addVerificationCert(publicKeyRing);

        OpenPgpMetadata metadata;
        try (DecryptionStream decryptionStream = PGPainless.decryptAndOrVerify()
                .onInputStream(new ByteArrayInputStream(encrypted.getBytes(StandardCharsets.UTF_8)))
                .withOptions(consumerOptions)) {
            int read;
            while ((read = decryptionStream.read()) != -1) {
                decryptedOut.write(read);
            }
            decryptionStream.close();
            metadata = decryptionStream.getResult();
        }

        String decryptedMessage = decryptedOut.toString(StandardCharsets.UTF_8);
        assertEquals(message, decryptedMessage, "Nach Entschlüsselung muss der Klartext identisch sein.");
        assertThat(metadata.isVerified()).isTrue();
        assertThat(metadata.getVerifiedSignatures()).hasSizeGreaterThanOrEqualTo(1);
    }

    private static String armorSecretKeyRing(PGPSecretKeyRing secretKeyRing) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ArmoredOutputStream armoredOut = new ArmoredOutputStream(out)) {
            secretKeyRing.encode(armoredOut);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static String armorPublicKeyRing(PGPPublicKeyRing publicKeyRing) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ArmoredOutputStream armoredOut = new ArmoredOutputStream(out)) {
            publicKeyRing.encode(armoredOut);
        }
        return out.toString(StandardCharsets.UTF_8);
    }
}
