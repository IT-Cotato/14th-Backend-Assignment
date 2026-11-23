package com.cotato.backend14th.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotato.backend14th.domain.auth.entity.TokenBlacklist;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    boolean existsByToken(String token);
}
