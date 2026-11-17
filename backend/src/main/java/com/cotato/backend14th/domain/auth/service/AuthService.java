package com.cotato.backend14th.domain.auth.service;

import com.cotato.backend14th.domain.auth.dto.LoginRequest;
import com.cotato.backend14th.domain.auth.dto.LoginResponse;
import com.cotato.backend14th.domain.auth.dto.SignupRequest;
import com.cotato.backend14th.domain.auth.dto.SignupResponse;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.jwt.JwtProvider;
import com.cotato.backend14th.global.jwt.TokenBlacklist;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// RequiredArgsConstructor -> 매개변수있는 생성자로 final이 붙은 필드에 대해서 생성자 생성 -> 이후 스프링이 사용
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;

    // DB 작업 트랜잭션으로 수행
    @Transactional
    public SignupResponse signup(SignupRequest request){

        // 해당 이메일이 존재하는 경우 -> 이미 존재하는 이메일이 다시한번 가입 시도를 한것으로 예외 발생
        if (userRepository.existsByEmail(request.getEmail())){
            throw new Backend14thException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.getEmail())
                // 비밀 번호를 암호화 해서 반환
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .nickname(request.getNickname())
                .generation(request.getGeneration())
                .part(request.getPart())
                .build();

        User saved = userRepository.save(user);
        return new SignupResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request){

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new Backend14thException(ErrorCode.INVALID_CREDENTIALS));

        // 입력한 비밀번호가 저장된 암호화 값과 맞는지 true 혹은 false로 알려주는 메서드
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new Backend14thException(ErrorCode.INVALID_CREDENTIALS);
        }

        return new LoginResponse(jwtProvider.createToken(user.getId()));

    }

    // 사용한 토큰을 만료 시각까지 차단 목록에 넣는다
    public void logout(String token){
        tokenBlacklist.add(token, jwtProvider.getExpiration(token));
    }
}
