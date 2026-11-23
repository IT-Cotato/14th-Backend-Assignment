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

import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(JwtTestConfiguration.class)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:me")
@AutoConfigureMockMvc
@Transactional
class UserControllerTests {

    private static final String ME_PATH = "/api/users/me";
    private static final String EMAIL = "me@example.com";
    private static final String PASSWORD = "sample-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginService loginService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void returnsOnlyProfileFieldsForAuthenticatedUser() throws Exception {
        User user = createUser();

        me(login())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", aMapWithSize(6)))
                .andExpect(jsonPath("$.data.userId").value(user.getId().intValue()))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.nickname").value("길동이"))
                .andExpect(jsonPath("$.data.generation").value(14))
                .andExpect(jsonPath("$.data.part").value("BE"));
    }

    @Test
    void rejectsMissingToken() throws Exception {
        expectUnauthorized(mockMvc.perform(get(ME_PATH)));
        mockMvc.perform(head(ME_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    @Test
    void rejectsTamperedToken() throws Exception {
        createUser();
        String token = login();
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        expectUnauthorized(me(token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1)));
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        User user = createUser();
        expectUnauthorized(me(signedToken(user.getId().toString(), Instant.now().minusSeconds(1))));
    }

    @Test
    void rejectsTokenAfterLogout() throws Exception {
        createUser();
        String token = login();

        me(token).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        expectUnauthorized(me(token));
    }

    @Test
    void rejectsTokensWithoutAResolvableUser() throws Exception {
        Instant expiresAt = Instant.now().plusSeconds(60);
        expectUnauthorized(me(signedToken("not-a-user-id", expiresAt)));
        expectUnauthorized(me(signedToken(Long.toString(Long.MAX_VALUE), expiresAt)));
    }

    private User createUser() {
        return userRepository.save(new User(EMAIL, passwordEncoder.encode(PASSWORD), "홍길동", "길동이", 14, Part.BE));
    }

    private String login() {
        return loginService.login(new LoginRequest(EMAIL, PASSWORD)).accessToken();
    }

    private ResultActions me(String token) throws Exception {
        return mockMvc.perform(get(ME_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String signedToken(String subject, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject(subject)
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private void expectUnauthorized(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-004"))
                .andExpect(jsonPath("$.path").value(ME_PATH));
    }
}
