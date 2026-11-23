package com.cotato.backend14th.domain.auth.entity;

import com.cotato.backend14th.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// 로그아웃으로 무효화한 JWT예요.
// 토큰 원문은 저장하지 않고 SHA-256 해시(hex 64자)만 저장해요.
@Entity
@Table(name = "revoked_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevokedToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    // 원래 토큰의 만료 시각이에요. 이 시각이 지나면 행을 지워도 돼요.
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public RevokedToken(String tokenHash, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }
}
