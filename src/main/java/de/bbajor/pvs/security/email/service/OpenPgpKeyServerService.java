package de.bbajor.pvs.security.email.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Service for retrieving OpenPGP public keys from keys.openpgp.org.
 * Supports both WKD (Web Key Directory) and HKP (HTTP Keyserver Protocol) methods.
 * 
 * <p>
 * According to https://keys.openpgp.org/about/usage/:
 * <ul>
 * <li>WKD: Used for automatic key discovery by email address</li>
 * <li>HKP: Used for manual key lookup</li>
 * </ul>
 * </p>
 */
@Service
public class OpenPgpKeyServerService {

    private static final Logger log = LoggerFactory.getLogger(OpenPgpKeyServerService.class);
    
    private static final String WKD_BASE_URL = "https://wkd.keys.openpgp.org";
    private static final String HKP_BASE_URL = "https://keys.openpgp.org";
    
    private final RestClient restClient;

    public OpenPgpKeyServerService() {
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", "PVS-Application/1.0")
                .build();
    }

    /**
     * Retrieves a public key for an email address using WKD (Web Key Directory).
     * This is the preferred method for automatic key discovery.
     * 
     * @param email the email address to look up
     * @return the ASCII-armored public key, or empty if not found
     */
    public Optional<String> lookupKeyByWkd(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            // WKD lookup via keys.openpgp.org
            // According to keys.openpgp.org documentation, WKD can be used via the lookup API
            // Format: https://wkd.keys.openpgp.org/lookup?op=get&search={url-encoded-email}
            String encodedEmail = URLEncoder.encode(email.trim(), StandardCharsets.UTF_8);
            String wkdUrl = WKD_BASE_URL + "/lookup?op=get&search=" + encodedEmail;
            
            log.debug("Looking up key via WKD for email: {}", email);
            String key = restClient.get()
                    .uri(wkdUrl)
                    .retrieve()
                    .body(String.class);

            if (key != null && key.trim().startsWith("-----BEGIN PGP PUBLIC KEY BLOCK-----")) {
                log.info("Successfully retrieved OpenPGP key via WKD for {}", email);
                return Optional.of(key.trim());
            } else {
                log.debug("No valid OpenPGP key found via WKD for {}", email);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.debug("Error retrieving key via WKD for {}: {}", email, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retrieves a public key for an email address using HKP (HTTP Keyserver Protocol).
     * This is a fallback method if WKD doesn't work.
     * 
     * @param email the email address to look up
     * @return the ASCII-armored public key, or empty if not found
     */
    public Optional<String> lookupKeyByHkp(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            // HKP URL format: https://keys.openpgp.org/vks/v1/by-email/{url-encoded-email}
            // According to keys.openpgp.org API documentation
            String encodedEmail = URLEncoder.encode(email.trim(), StandardCharsets.UTF_8);
            String hkpUrl = HKP_BASE_URL + "/vks/v1/by-email/" + encodedEmail;
            
            log.debug("Looking up key via HKP for email: {}", email);
            String key = restClient.get()
                    .uri(hkpUrl)
                    .retrieve()
                    .body(String.class);

            if (key != null && key.trim().startsWith("-----BEGIN PGP PUBLIC KEY BLOCK-----")) {
                log.info("Successfully retrieved OpenPGP key via HKP for {}", email);
                return Optional.of(key.trim());
            } else {
                log.debug("No valid OpenPGP key found via HKP for {}", email);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.debug("Error retrieving key via HKP for {}: {}", email, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retrieves a public key for an email address, trying WKD first, then HKP as fallback.
     * 
     * @param email the email address to look up
     * @return the ASCII-armored public key, or empty if not found
     */
    public Optional<String> lookupKey(String email) {
        // Try WKD first (preferred method)
        Optional<String> key = lookupKeyByWkd(email);
        if (key.isPresent()) {
            return key;
        }

        // Fallback to HKP
        log.debug("WKD lookup failed, trying HKP for {}", email);
        return lookupKeyByHkp(email);
    }
}

