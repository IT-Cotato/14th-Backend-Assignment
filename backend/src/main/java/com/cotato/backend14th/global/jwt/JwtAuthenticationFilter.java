package com.cotato.backend14th.global.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// 요청마다 Authorization 헤더의 토큰을 검사해서, 올바르면 "로그인한 사용자"로 등록한다.
// 토큰이 없거나 틀려도 여기서 막지 않고 통과시킨다. (막는 건 SecurityConfig의 접근 규칙이 한다)
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;

    // 모든 요청이 이 필터를 먼저 지나가는 기능
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            // 서명이 맞고 만료되지 않았으며, 로그아웃으로 차단된 토큰이 아닐 때만 인증한다
            if (jwtProvider.validateToken(token) && !tokenBlacklist.isBlacklisted(token)) {
                Long userId = jwtProvider.getUserId(token);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
