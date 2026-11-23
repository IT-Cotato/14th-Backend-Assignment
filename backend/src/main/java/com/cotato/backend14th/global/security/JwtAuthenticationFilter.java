package com.cotato.backend14th.global.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cotato.backend14th.domain.auth.service.TokenBlacklistService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Authorization 헤더의 토큰이 유효하고 로그아웃되지 않았으면 인증 정보를 등록해요.
 * 토큰이 없거나 잘못됐으면 익명 사용자로 넘기고, 로그인이 필요한 API에서 401로 막혀요.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            jwtTokenProvider.parse(token)
                    .filter(info -> !tokenBlacklistService.isBlacklisted(token))
                    .ifPresent(info -> SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(info.userId(), token, List.of())));
        }
        filterChain.doFilter(request, response);
    }

    public static String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
