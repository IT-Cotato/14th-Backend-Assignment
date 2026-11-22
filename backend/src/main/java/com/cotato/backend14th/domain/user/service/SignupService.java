package com.cotato.backend14th.domain.user.service;

import com.cotato.backend14th.domain.user.dto.SignupRequest;
import com.cotato.backend14th.domain.user.dto.SignupResponse;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupResponse signup(SignupRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }

        String email = request.email();
        if (userRepository.existsByEmail(email)) {
            throw new Backend14thException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User(email, passwordEncoder.encode(request.password()), request.name(),
                request.nickname(), request.generation(), request.part());

        try {
            return new SignupResponse(userRepository.save(user).getId());
        } catch (DataIntegrityViolationException e) {
            // The repository transaction has rolled back before checking for a concurrent signup.
            if (userRepository.existsByEmail(email)) {
                throw new Backend14thException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            throw e;
        }
    }
}
