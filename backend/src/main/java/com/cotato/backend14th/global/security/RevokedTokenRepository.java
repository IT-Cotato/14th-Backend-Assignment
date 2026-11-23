package com.cotato.backend14th.global.security;

import java.time.Instant;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {
    @Modifying
    @Query(value = "INSERT INTO revoked_token (jti, expires_at) VALUES (:jti, :expires) ON DUPLICATE KEY UPDATE jti = jti", nativeQuery = true)
    void revoke(@Param("jti") String jti, @Param("expires") Instant expires);

    void deleteByExpiresAtBefore(Instant now);
}
