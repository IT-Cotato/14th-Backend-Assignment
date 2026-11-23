// JwtAuthenticationFilter.java
package com.cotato.backend14th.global.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cotato.backend14th.domain.auth.repository.LogoutTokenRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final LogoutTokenRepository logoutTokenRepository;

    // 유효하고 로그아웃되지 않은 토큰이면 userId를 인증 정보로 넣어요.
    // 토큰이 없거나 유효하지 않으면 익명으로 통과시키고, 인증이 필요한 경로는 EntryPoint가 401을 내려줘요.
    // 그래서 공개 API에서도 토큰이 있으면 작성자 여부(isAuthor)를 계산할 수 있어요.
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        jwtProvider.resolveToken(request)
                .filter(token -> !logoutTokenRepository.existsByToken(token))
                .flatMap(jwtProvider::getUserId)
                .ifPresent(userId -> SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId, null, List.of())));

        filterChain.doFilter(request, response);
    }
}
