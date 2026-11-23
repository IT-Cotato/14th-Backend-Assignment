
package com.cotato.backend14th.domain.auth.service;

import com.cotato.backend14th.domain.auth.dto.LoginRequest;
import com.cotato.backend14th.domain.auth.dto.LoginResponse;
import com.cotato.backend14th.domain.auth.dto.SignupRequest;
import com.cotato.backend14th.domain.auth.dto.SignupResponse;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final TokenRevocationService tokenRevocationService;

    // 회원가입
    @Transactional
    public SignupResponse signup(SignupRequest request) {

        // 1. 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw new Backend14thException(
                    ErrorCode.EMAIL_ALREADY_EXISTS
            );
        }

        // 2. 비밀번호 암호화
        String encodedPassword =
                passwordEncoder.encode(request.password());

        // 3. User 엔티티 생성
        User user = new User(
                request.email(),
                encodedPassword,
                request.name(),
                request.nickname(),
                request.generation(),
                request.part()
        );

        // 4. 데이터베이스에 저장
        User savedUser = userRepository.save(user);

        // 5. 회원가입 결과 반환
        return new SignupResponse(savedUser.getId());
    }

    // 로그인
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new Backend14thException(
                        ErrorCode.INVALID_CREDENTIALS
                ));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )) {
            throw new Backend14thException(
                    ErrorCode.INVALID_CREDENTIALS
            );
        }

        // 3. JWT 발급 시간과 만료 시간 설정
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(3600);

        // 4. JWT에 포함할 정보 설정
        // jti(고유 ID)를 넣어 같은 초에 다시 로그인해도 토큰이 매번 달라지게 해요.
        // 없으면 로그아웃 직후 재로그인한 토큰이 무효화된 토큰과 같아져 401이 나요.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("email", user.getEmail())
                .build();

        // 5. JWT 서명 알고리즘 설정
        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        // 6. JWT 생성
        String accessToken = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        // 7. 로그인 응답 반환
        return new LoginResponse(accessToken, "Bearer");
    }

    // 로그아웃
    // 현재 요청의 JWT를 만료 시각까지 무효화해요.
    @Transactional
    public void logout(Jwt jwt) {
        tokenRevocationService.revoke(
                jwt.getTokenValue(),
                jwt.getExpiresAt()
        );
    }
}
