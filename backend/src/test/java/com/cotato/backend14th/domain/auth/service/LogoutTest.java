package com.cotato.backend14th.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.security.JwtAuthenticationFilter;
import com.cotato.backend14th.global.security.JwtTokenProvider;
import com.cotato.backend14th.global.security.TokenBlacklist;

// 실제 JwtTokenProvider, TokenBlacklist, 필터를 연결해서 로그아웃한 토큰이 무효가 되는지 확인해요.
// 테스트용 키는 실행할 때마다 무작위로 만들어요. (코드에 시크릿을 적지 않아요)
class LogoutTest {

    private JwtTokenProvider jwtTokenProvider;
    private TokenBlacklist tokenBlacklist;
    private AuthService authService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        byte[] randomBytes = new byte[48];
        new SecureRandom().nextBytes(randomBytes);
        String randomSecret = Base64.getEncoder().encodeToString(randomBytes);

        jwtTokenProvider = new JwtTokenProvider(randomSecret, 60_000L);
        tokenBlacklist = new TokenBlacklist();
        authService = new AuthService(mock(UserRepository.class), mock(PasswordEncoder.class),
                jwtTokenProvider, tokenBlacklist);
        filter = new JwtAuthenticationFilter(jwtTokenProvider, tokenBlacklist);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("로그아웃 전에는 토큰으로 인증되고, 로그아웃 후에는 같은 토큰으로 인증되지 않는다")
    void logout_invalidatesToken() throws Exception {
        String token = jwtTokenProvider.createAccessToken(1L);

        Authentication before = authenticate(token);
        assertThat(before).isNotNull();
        assertThat(before.getPrincipal()).isEqualTo(1L);

        authService.logout(token);

        assertThat(tokenBlacklist.contains(token)).isTrue();
        assertThat(authenticate(token)).isNull();
    }

    @Test
    @DisplayName("로그아웃은 해당 토큰만 무효로 만들고, 새로 발급한 토큰은 그대로 쓸 수 있다")
    void logout_onlyAffectsThatToken() throws Exception {
        String loggedOut = jwtTokenProvider.createAccessToken(1L);
        authService.logout(loggedOut);

        // 발급 시각(초 단위)이 같으면 토큰 문자열이 같아질 수 있어서 다른 사용자로 발급해요
        String other = jwtTokenProvider.createAccessToken(2L);

        assertThat(authenticate(loggedOut)).isNull();
        assertThat(authenticate(other)).isNotNull();
    }

    private Authentication authenticate(String token) throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
