package com.cotato.backend14th.domain.auth.repository;

import com.cotato.backend14th.domain.auth.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByTokenHash(String tokenHash);
}
