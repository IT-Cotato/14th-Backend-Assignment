package com.cotato.backend14th.global.security;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 유효한 토큰이면 SecurityContext에 userId를 넣어요.
 * 토큰이 없거나 잘못돼도 여기서 바로 401을 내리지 않아요.
 * 공개 API는 익명으로 통과시키고, 로그인 API는 JwtAuthenticationEntryPoint가 401을 내려줘요.
 * 서블릿 필터로 이중 등록되지 않도록 @Component를 붙이지 않고 SecurityConfig에서 직접 만들어요.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = jwtProvider.resolveToken(request);

        if (token != null && jwtProvider.validate(token) && !tokenBlacklist.contains(token)) {
            Long userId = jwtProvider.getUserId(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
