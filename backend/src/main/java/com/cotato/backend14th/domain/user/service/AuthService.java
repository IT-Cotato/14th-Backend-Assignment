package com.cotato.backend14th.domain.user.service;

import com.cotato.backend14th.domain.user.dto.AuthDtos.*;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.security.TokenService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.cotato.backend14th.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final TokenService tokens;

    public UserId signup(Signup request) {
        if (!validPasswordSize(request.password())) {
            throw new Backend14thException(INVALID_INPUT);
        }
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new Backend14thException(EMAIL_ALREADY_EXISTS);
        }
        try {
            User user = users.saveAndFlush(new User(email, passwords.encode(request.password()), request.name(),
                    request.nickname(), request.generation(), request.part()));
            return new UserId(user.getId());
        } catch (DataIntegrityViolationException error) {
            if (users.existsByEmail(email)) {
                throw new Backend14thException(EMAIL_ALREADY_EXISTS);
            }
            throw error;
        }
    }

    @Transactional(readOnly = true)
    public Token login(Login request) {
        if (request.email() == null || !validPasswordSize(request.password())) {
            throw new Backend14thException(INVALID_CREDENTIALS);
        }
        User user = users.findByEmail(normalize(request.email()))
                .filter(found -> passwords.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new Backend14thException(INVALID_CREDENTIALS));
        return new Token(tokens.issue(user.getId()), "Bearer");
    }

    @Transactional(readOnly = true)
    public Profile profile(long id) {
        return users.findById(id).map(Profile::from).orElseThrow(() -> new Backend14thException(UNAUTHORIZED));
    }

    private static boolean validPasswordSize(String password) {
        return password != null && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
