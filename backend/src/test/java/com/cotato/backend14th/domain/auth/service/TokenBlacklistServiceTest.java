package com.cotato.backend14th.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.auth.repository.BlacklistedTokenRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TokenBlacklistServiceTest {

    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Test
    void 로그아웃한_토큰은_블랙리스트에서_찾을_수_있고_원문은_저장하지_않는다() {
        tokenBlacklistService.blacklist("token-a", Instant.now().plusSeconds(600));

        assertThat(tokenBlacklistService.isBlacklisted("token-a")).isTrue();
        assertThat(tokenBlacklistService.isBlacklisted("token-b")).isFalse();
        assertThat(blacklistedTokenRepository.findAll())
                .singleElement()
                .satisfies(saved -> assertThat(saved.getTokenHash()).hasSize(64).isNotEqualTo("token-a"));
    }

    @Test
    void 같은_토큰을_두_번_로그아웃해도_한_번만_저장한다() {
        Instant expiresAt = Instant.now().plusSeconds(600);
        tokenBlacklistService.blacklist("token-a", expiresAt);
        tokenBlacklistService.blacklist("token-a", expiresAt);

        assertThat(blacklistedTokenRepository.count()).isEqualTo(1);
    }

    @Test
    void 만료된_기록은_정리한다() {
        tokenBlacklistService.blacklist("expired", Instant.now().minusSeconds(60));
        tokenBlacklistService.blacklist("alive", Instant.now().plusSeconds(600));

        tokenBlacklistService.removeExpired();

        assertThat(tokenBlacklistService.isBlacklisted("expired")).isFalse();
        assertThat(tokenBlacklistService.isBlacklisted("alive")).isTrue();
    }
}
