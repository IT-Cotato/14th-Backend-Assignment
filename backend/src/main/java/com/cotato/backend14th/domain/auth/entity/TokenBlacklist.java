package com.cotato.backend14th.domain.auth.entity;

import java.time.LocalDateTime;

import com.cotato.backend14th.global.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 로그아웃한 accessToken을 원래 만료 시각까지 무효로 처리하기 위해 저장해요
@Entity
@Table(name = "token_blacklist")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenBlacklist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public TokenBlacklist(String token, LocalDateTime expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }
}
