package com.cotato.backend14th.domain.auth.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotato.backend14th.domain.auth.entity.BlacklistedToken;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    boolean existsByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from BlacklistedToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
