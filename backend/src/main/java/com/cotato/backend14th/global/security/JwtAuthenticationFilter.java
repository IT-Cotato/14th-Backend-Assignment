package com.cotato.backend14th.global.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cotato.backend14th.domain.auth.repository.BlacklistedTokenRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 유효하고 로그아웃되지 않은 토큰이면 SecurityContext에 userId를 principal로 넣어요.
 * 토큰이 없거나 잘못됐으면 아무것도 하지 않고 넘겨요 → 공개 API는 비로그인으로 동작하고,
 * 로그인 API는 JwtAuthenticationEntryPoint가 401을 내려줘요.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = JwtTokenProvider.resolveToken(request);
        if (token != null && jwtTokenProvider.isValid(token) && !blacklistedTokenRepository.existsByToken(token)) {
            Long userId = jwtTokenProvider.getUserId(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }
}
