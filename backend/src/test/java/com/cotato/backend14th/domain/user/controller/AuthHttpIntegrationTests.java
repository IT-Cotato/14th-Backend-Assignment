package com.cotato.backend14th.domain.user.controller;

import com.cotato.backend14th.JwtTestConfiguration;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Import(JwtTestConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:auth-http")
class AuthHttpIntegrationTests {

    private static final String SIGNUP = "/api/auth/signup";
    private static final String LOGIN = "/api/auth/login";
    private static final String ME = "/api/users/me";
    private static final String LOGOUT = "/api/auth/logout";
    private static final String PASSWORD = "sample-password";
    private static final String TIMESTAMP_PATTERN = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void completesSignupLoginProfileAndLogoutOverHttp() {
        String email = "lifecycle@example.com";
        JsonNode signup = expectSuccess(http.postForEntity(SIGNUP, signupBody(email, PASSWORD), JsonNode.class), 201);
        long userId = signup.path("userId").asLong();
        assertThat(userId).isPositive();
        assertThat(signup.size()).isEqualTo(1);
        String storedPassword = users.findById(userId).orElseThrow().getPassword();
        assertThat(storedPassword).isNotEqualTo(PASSWORD);
        assertThat(new BCryptPasswordEncoder().matches(PASSWORD, storedPassword)).isTrue();

        JsonNode login = expectSuccess(http.postForEntity(LOGIN, credentials(email, PASSWORD), JsonNode.class), 200);
        assertThat(login.path("tokenType").asText()).isEqualTo("Bearer");
        String token = login.path("accessToken").asText();
        assertThat(token.split("\\.")).hasSize(3);

        JsonNode profile = expectSuccess(http.exchange(ME, HttpMethod.GET, bearer(token), JsonNode.class), 200);
        assertThat(profile).isEqualTo(objectMapper.valueToTree(Map.of(
                "userId", signup.path("userId"),
                "email", email,
                "name", "홍길동",
                "nickname", "길동이",
                "generation", 14,
                "part", "BE"
        )));

        ResponseEntity<byte[]> logout = http.exchange(LOGOUT, HttpMethod.POST, bearer(token), byte[].class);
        assertThat(logout.getStatusCode().value()).isEqualTo(204);
        assertThat(logout.getBody()).isNullOrEmpty();
        assertThat(logout.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).isNull();

        expectError(http.exchange(ME, HttpMethod.GET, bearer(token), JsonNode.class),
                401, "COMMON-004", HttpMethod.GET, ME);
    }

    @Test
    void protectedEndpointsRejectMissingTamperedAndExpiredTokensOverHttp() {
        expectProtectedRoutesUnauthorized(HttpEntity.EMPTY);
        Instant now = Instant.now();
        String token = signedToken(now, now.plusSeconds(60));
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
        expectProtectedRoutesUnauthorized(bearer(tampered));
        expectProtectedRoutesUnauthorized(bearer(signedToken(now.minusSeconds(3600), now.minusSeconds(1))));
    }

    @Test
    void publicEndpointsReturnSpecifiedBusinessErrorsOverHttp() {
        String email = "business-errors@example.com";
        expectSuccess(http.postForEntity(SIGNUP, signupBody(email, PASSWORD), JsonNode.class), 201);
        expectError(http.postForEntity(SIGNUP, signupBody(email, PASSWORD), JsonNode.class),
                409, "USER-001", HttpMethod.POST, SIGNUP);
        expectError(http.postForEntity(SIGNUP, signupBody("invalid-email", PASSWORD), JsonNode.class),
                400, "COMMON-003", HttpMethod.POST, SIGNUP);
        expectError(http.postForEntity(LOGIN, credentials(email, "wrong-password"), JsonNode.class),
                401, "USER-002", HttpMethod.POST, LOGIN);
    }

    private void expectProtectedRoutesUnauthorized(HttpEntity<?> request) {
        ResponseEntity<JsonNode> profile = http.exchange(ME, HttpMethod.GET, request, JsonNode.class);
        ResponseEntity<JsonNode> logout = http.exchange(LOGOUT, HttpMethod.POST, request, JsonNode.class);
        expectError(profile, 401, "COMMON-004", HttpMethod.GET, ME);
        expectError(logout, 401, "COMMON-004", HttpMethod.POST, LOGOUT);
        assertThat(profile.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(logout.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
    }

    private JsonNode expectSuccess(ResponseEntity<JsonNode> response, int status) {
        JsonNode body = expectEnvelope(response, status, true);
        assertThat(body.size()).isEqualTo(4);
        assertThat(body.path("data").isObject()).isTrue();
        return body.path("data");
    }

    private void expectError(ResponseEntity<JsonNode> response, int status, String code, HttpMethod method, String path) {
        JsonNode body = expectEnvelope(response, status, false);
        assertThat(body.size()).isEqualTo(7);
        assertThat(body.path("code").asText()).isEqualTo(code);
        assertThat(body.path("message").asText()).isNotBlank();
        assertThat(body.path("method").asText()).isEqualTo(method.name());
        assertThat(body.path("path").asText()).isEqualTo(path);
    }

    private JsonNode expectEnvelope(ResponseEntity<JsonNode> response, int status, boolean success) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.path("success").isBoolean()).isTrue();
        assertThat(body.path("success").booleanValue()).isEqualTo(success);
        assertThat(body.path("status").intValue()).isEqualTo(status);
        assertThat(body.path("timestamp").asText()).matches(TIMESTAMP_PATTERN);
        return body;
    }

    private HttpEntity<Void> bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private String signedToken(Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("1").issuedAt(issuedAt).expiresAt(expiresAt).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private Map<String, Object> signupBody(String email, String password) {
        return Map.of("email", email, "password", password, "name", "홍길동",
                "nickname", "길동이", "generation", 14, "part", "BE");
    }

    private Map<String, String> credentials(String email, String password) {
        return Map.of("email", email, "password", password);
    }
}
