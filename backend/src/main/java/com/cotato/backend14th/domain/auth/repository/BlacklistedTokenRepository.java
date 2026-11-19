package com.cotato.backend14th.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotato.backend14th.domain.auth.entity.BlacklistedToken;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    boolean existsByToken(String token);
}
