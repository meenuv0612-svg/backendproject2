package com.glowora.security;

import com.glowora.entity.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * Stateless HMAC-SHA256 authentication token service for Glowora.
 * Works alongside HttpSession cookies so that authentication persists reliably
 * across localhost, 127.0.0.1, Live Server ports, page refreshes, and file:// origins.
 */
@Component
@Slf4j
public class GloworaTokenService {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final long TOKEN_VALIDITY_SECONDS = 7L * 24 * 60 * 60; // 7 days

    @Value("${glowora.security.token-secret:GloworaLuxuryBeautySecretKey2026!@#SpringSecurityHmac}")
    private String tokenSecret;

    public String generateToken(Customer customer) {
        long expiresAt = Instant.now().getEpochSecond() + TOKEN_VALIDITY_SECONDS;
        String role = (customer.getRole() != null && !customer.getRole().isBlank())
                ? customer.getRole()
                : "ROLE_CUSTOMER";
        String payload = customer.getId() + "|" + customer.getEmail().toLowerCase() + "|" + role + "|" + expiresAt;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return encodedPayload + "." + signature;
    }

    public TokenClaims validateAndParse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String trimmed = token.trim();
        int dotIndex = trimmed.indexOf('.');
        if (dotIndex <= 0 || dotIndex >= trimmed.length() - 1) {
            return null;
        }
        String encodedPayload = trimmed.substring(0, dotIndex);
        String providedSig = trimmed.substring(dotIndex + 1);
        String expectedSig = sign(encodedPayload);

        if (!MessageDigest.isEqual(
                providedSig.getBytes(StandardCharsets.UTF_8),
                expectedSig.getBytes(StandardCharsets.UTF_8))) {
            return null;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|");
            if (parts.length != 4) {
                return null;
            }
            Long customerId = Long.parseLong(parts[0]);
            String email = parts[1];
            String role = parts[2];
            long expiresAt = Long.parseLong(parts[3]);

            if (Instant.now().getEpochSecond() > expiresAt) {
                return null;
            }
            return new TokenClaims(customerId, email, role, expiresAt);
        } catch (Exception ex) {
            log.debug("Invalid Glowora auth token format: {}", ex.getMessage());
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(
                    tokenSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGO
            );
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign Glowora authentication token", e);
        }
    }

    public record TokenClaims(Long customerId, String email, String role, long expiresAtEpochSeconds) {}
}
