package com.cotato.backend14th.domain.user.controller;

import com.cotato.backend14th.JwtTestConfiguration;
import com.cotato.backend14th.domain.user.dto.LoginRequest;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.domain.user.service.LoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(JwtTestConfiguration.class)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:logout")
@AutoConfigureMockMvc
@Transactional
class LogoutControllerTests {

    private static final String LOGOUT_PATH = "/api/auth/logout";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void logsOutWithoutResponseBodyAndRejectsReuse() throws Exception {
        String email = "logout@example.com";
        String password = "sample-password";
        userRepository.save(new User(email, passwordEncoder.encode(password), "홍길동", "길동이", 14, Part.BE));
        LoginRequest request = new LoginRequest(email, password);
        String firstToken = loginService.login(request).accessToken();
        String secondToken = loginService.login(request).accessToken();

        logout(firstToken)
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        expectUnauthorized(logout(firstToken));
        logout(secondToken).andExpect(status().isNoContent());
    }

    @Test
    void rejectsMissingToken() throws Exception {
        expectUnauthorized(mockMvc.perform(post(LOGOUT_PATH)));
    }

    @Test
    void rejectsMalformedToken() throws Exception {
        expectUnauthorized(logout("not-a-jwt"));
    }

    @Test
    void rejectsTamperedSignature() throws Exception {
        String token = signedToken(Instant.now().plusSeconds(60));
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        expectUnauthorized(logout(token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1)));
    }

    @Test
    void rejectsExpiredTokenWithoutClockSkew() throws Exception {
        expectUnauthorized(logout(signedToken(Instant.now().minusSeconds(1))));
    }

    private String signedToken(Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("123")
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private ResultActions logout(String token) throws Exception {
        return mockMvc.perform(post(LOGOUT_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private void expectUnauthorized(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-004"))
                .andExpect(jsonPath("$.path").value(LOGOUT_PATH));
    }
}
