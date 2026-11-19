package com.cotato.backend14th.global.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cotato.backend14th.domain.auth.repository.TokenBlacklistRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// 요청마다 Authorization 헤더의 토큰을 확인해서 인증 정보를 넣어요.
// 토큰이 없거나 잘못됐으면 인증 없이 그대로 통과시켜요.
// 공개 API는 그대로 동작하고, 로그인이 필요한 API는 EntryPoint가 401을 내려줘요.
// SecurityConfig에서 직접 생성해서 등록해요. (@Component로 만들면 서블릿 필터로도 한 번 더 등록돼요)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        String token = jwtProvider.resolveToken(request.getHeader(HttpHeaders.AUTHORIZATION));

        if (token != null && jwtProvider.isValid(token) && !tokenBlacklistRepository.existsByToken(token)) {
            Long userId = jwtProvider.getUserId(token);
            // principal에 userId를 넣어요. Controller에서 @AuthenticationPrincipal Long userId로 꺼내요.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
