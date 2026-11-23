package com.cotato.backend14th.domain.auth.service;

import com.cotato.backend14th.domain.auth.entity.RevokedToken;
import com.cotato.backend14th.domain.auth.repository.RevokedTokenRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TokenRevocationServiceTest {

    private static final String TOKEN = "header.payload.signature";

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @InjectMocks
    private TokenRevocationService tokenRevocationService;

    @Test
    @DisplayName("해시는 SHA-256 hex 64자이고 같은 토큰이면 같은 값이에요")
    void hash_isSha256Hex() {
        String hash = TokenRevocationService.hash(TOKEN);

        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(hash).isEqualTo(TokenRevocationService.hash(TOKEN));
        assertThat(hash).isNotEqualTo(TokenRevocationService.hash(TOKEN + "x"));
        // "abc"의 SHA-256 표준 테스트 벡터와 일치하는지 확인해요
        assertThat(TokenRevocationService.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    @DisplayName("무효화할 때 토큰 원문이 아니라 해시와 만료 시각을 저장해요")
    void revoke_savesHashOnly() {
        Instant expiresAt = Instant.now().plusSeconds(3600);
        given(revokedTokenRepository.existsByTokenHash(any())).willReturn(false);

        tokenRevocationService.revoke(TOKEN, expiresAt);

        ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
        then(revokedTokenRepository).should().save(captor.capture());
        assertThat(captor.getValue().getTokenHash())
                .isEqualTo(TokenRevocationService.hash(TOKEN))
                .doesNotContain(TOKEN);
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("이미 무효화된 토큰은 다시 저장하지 않아요")
    void revoke_alreadyRevoked() {
        given(revokedTokenRepository.existsByTokenHash(TokenRevocationService.hash(TOKEN)))
                .willReturn(true);

        tokenRevocationService.revoke(TOKEN, Instant.now().plusSeconds(3600));

        then(revokedTokenRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("해시로 무효화 여부를 조회해요")
    void isRevoked() {
        given(revokedTokenRepository.existsByTokenHash(TokenRevocationService.hash(TOKEN)))
                .willReturn(true);

        assertThat(tokenRevocationService.isRevoked(TOKEN)).isTrue();
        assertThat(tokenRevocationService.isRevoked("other.token.value")).isFalse();
    }
}
