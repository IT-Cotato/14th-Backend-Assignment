package com.cotato.backend14th.domain.auth.service;

import com.cotato.backend14th.domain.auth.entity.RevokedToken;
import com.cotato.backend14th.domain.auth.repository.RevokedTokenRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

// JWT 무효화(로그아웃) 정보를 DB에 저장하고 조회해요.
// 토큰 원문 대신 SHA-256 해시를 쓰기 때문에 jti가 없는 기존 토큰도 무효화할 수 있어요.
@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;

    // 토큰을 무효화해요. 이미 무효화된 토큰이면 다시 저장하지 않아요.
    @Transactional
    public void revoke(String tokenValue, Instant expiresAt) {
        String tokenHash = hash(tokenValue);

        if (revokedTokenRepository.existsByTokenHash(tokenHash)) {
            return;
        }

        revokedTokenRepository.save(new RevokedToken(tokenHash, expiresAt));
    }

    // 무효화된 토큰인지 확인해요.
    @Transactional(readOnly = true)
    public boolean isRevoked(String tokenValue) {
        return revokedTokenRepository.existsByTokenHash(hash(tokenValue));
    }

    // 토큰 원문을 SHA-256 hex 문자열(64자)로 바꿔요.
    static String hash(String tokenValue) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(tokenValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 JVM에서 지원해야 하는 알고리즘이라 여기 오지 않아요.
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
