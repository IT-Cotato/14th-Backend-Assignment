package com.cotato.backend14th.global.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RevokedTokens {

    private final ConcurrentMap<String, Instant> expirations = new ConcurrentHashMap<>();

    public boolean revoke(Jwt jwt) {
        Instant now = Instant.now();
        expirations.values().removeIf(expiration -> !expiration.isAfter(now));
        return jwt.getExpiresAt().isAfter(now)
                && expirations.putIfAbsent(fingerprint(jwt), jwt.getExpiresAt()) == null;
    }

    public boolean isRevoked(Jwt jwt) {
        return expirations.containsKey(fingerprint(jwt));
    }

    private String fingerprint(Jwt jwt) {
        String token = jwt.getTokenValue();
        // The verified signing input is stable across equivalent signature encodings.
        String signingInput = token.substring(0, token.lastIndexOf('.'));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(signingInput.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
