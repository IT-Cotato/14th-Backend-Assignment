package com.cotato.backend14th.global.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cotato.backend14th.domain.auth.repository.RevokedTokenRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.security.JwtProvider.ParsedToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// 요청마다 토큰을 확인해서 유효하면 로그인한 사용자로 인식시켜요.
// 토큰이 없거나 유효하지 않으면 인증 없이 통과시키고, 로그인이 필요한 API는
// SecurityConfig의 접근 규칙과 JwtAuthenticationEntryPoint가 401로 막아요.
// 이렇게 해야 공개 API(모집글 상세 등)에서 토큰이 있을 때만 isAuthor를 계산할 수 있어요.
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RevokedTokenRepository revokedTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = BearerToken.resolve(request);

        if (token != null) {
            try {
                ParsedToken parsed = jwtProvider.parse(token);
                if (!revokedTokenRepository.existsByJti(parsed.jti())) {
                    // principal에 사용자 ID를 담아요. 컨트롤러에서 @AuthenticationPrincipal Long 으로 꺼내요.
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(parsed.userId(), null, List.of()));
                }
            } catch (Backend14thException ignored) {
                // 유효하지 않은 토큰은 인증 없는 요청으로 취급해요
            }
        }

        filterChain.doFilter(request, response);
    }
}
