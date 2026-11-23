package com.cotato.backend14th.domain.user.controller;

import com.cotato.backend14th.JwtTestConfiguration;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(JwtTestConfiguration.class)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:login")
@AutoConfigureMockMvc
@Transactional
class LoginControllerTests {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String EMAIL = "member@example.com";
    private static final String PASSWORD = "sample-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${JWT_SIGNING_KEY}")
    private String signingKey;

    private Long userId;

    @BeforeEach
    void createUser() {
        User user = new User(EMAIL, passwordEncoder.encode(PASSWORD), "홍길동", "길동이", 14, Part.BE);
        userId = userRepository.save(user).getId();
    }

    @Test
    void logsInWithoutAuthenticationAndReturnsSignedAccessToken() throws Exception {
        String response = login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).at("/data/accessToken").asText();
        byte[] bytes = Base64.getDecoder().decode(signingKey);
        Jwt jwt = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(bytes, "HmacSHA256")).build().decode(token);

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void rejectsUnknownEmail() throws Exception {
        expectInvalidCredentials(login("unknown@example.com", PASSWORD));
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        expectInvalidCredentials(login(EMAIL, "wrong-password"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456789", "true", "{}", "[]"})
    void rejectsNonStringCredentialsDuringDeserialization(String jsonValue) throws Exception {
        for (String field : new String[]{"email", "password"}) {
            ObjectNode request = objectMapper.createObjectNode().put("email", EMAIL).put("password", PASSWORD);
            request.set(field, objectMapper.readTree(jsonValue));
            ResultActions response = mockMvc.perform(post(LOGIN_PATH)
                    .contentType(MediaType.APPLICATION_JSON).content(request.toString()));
            expectInvalidCredentials(response);
            response.andExpect(result -> assertThat(result.getResolvedException())
                    .isInstanceOf(HttpMessageNotReadableException.class));
        }
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post(LOGIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private void expectInvalidCredentials(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER-002"))
                .andExpect(jsonPath("$.path").value(LOGIN_PATH))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());
    }
}
