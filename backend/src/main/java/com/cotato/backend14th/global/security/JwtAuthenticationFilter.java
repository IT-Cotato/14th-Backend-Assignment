package com.cotato.backend14th.global.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// Bearer 토큰이 유효하고 로그아웃한 토큰이 아니면 인증 정보를 넣어요. principal은 userId(Long)예요.
// 토큰이 없거나 무효이면 아무것도 하지 않고 익명 사용자로 넘겨요.
// 로그인이 필요한 API라면 JwtAuthenticationEntryPoint가 401을 내려줘요.
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        jwtTokenProvider.resolveToken(request)
                .filter(token -> !tokenBlacklist.contains(token))
                .flatMap(jwtTokenProvider::getUserId)
                .ifPresent(userId -> SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId, null, List.of())));

        filterChain.doFilter(request, response);
    }
}
