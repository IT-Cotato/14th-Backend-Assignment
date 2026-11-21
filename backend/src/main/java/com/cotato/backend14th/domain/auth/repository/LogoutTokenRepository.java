// LogoutTokenRepository.java
package com.cotato.backend14th.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotato.backend14th.domain.auth.entity.LogoutToken;

public interface LogoutTokenRepository extends JpaRepository<LogoutToken, Long> {

    boolean existsByToken(String token);
}
