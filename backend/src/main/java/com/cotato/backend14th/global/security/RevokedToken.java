package com.cotato.backend14th.global.security;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "revoked_token", indexes = @Index(name = "idx_revoked_token_expires", columnList = "expires_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevokedToken {
    @Id @Column(length = 36)
    private String jti;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
