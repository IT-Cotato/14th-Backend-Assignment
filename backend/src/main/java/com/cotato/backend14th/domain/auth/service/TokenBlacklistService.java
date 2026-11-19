package com.cotato.backend14th.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.auth.entity.BlacklistedToken;
import com.cotato.backend14th.domain.auth.repository.BlacklistedTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenBlacklistService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Transactional
    public void blacklist(String token, Instant expiresAt) {
        String hash = hash(token);
        if (!blacklistedTokenRepository.existsByTokenHash(hash)) {
            LocalDateTime expiry = LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault());
            blacklistedTokenRepository.save(new BlacklistedToken(hash, expiry));
        }
    }

    public boolean isBlacklisted(String token) {
        return blacklistedTokenRepository.existsByTokenHash(hash(token));
    }

    // 만료된 토큰은 어차피 거절되므로 한 시간마다 정리해요
    @Scheduled(fixedDelay = 60 * 60 * 1000L)
    @Transactional
    public void removeExpired() {
        blacklistedTokenRepository.deleteExpired(LocalDateTime.now());
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
