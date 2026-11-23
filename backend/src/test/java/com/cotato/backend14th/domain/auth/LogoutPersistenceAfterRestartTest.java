package com.cotato.backend14th.domain.auth;

import com.cotato.backend14th.Backend14thApplication;
import com.cotato.backend14th.domain.auth.entity.RevokedToken;
import com.cotato.backend14th.domain.auth.repository.RevokedTokenRepository;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 서버를 실제로 두 번 띄워서, 로그아웃한 JWT가 재시작 후에도 계속 401인지 확인해요.
// 무효화 정보가 메모리가 아니라 DB(revoked_token)에 남는지 검증하는 테스트예요.
// 테스트가 만든 회원과 무효화 행은 마지막에 지워요.
class LogoutPersistenceAfterRestartTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("로그아웃한 토큰은 서버를 재시작한 뒤에도 401이에요")
    void revokedTokenStaysRevokedAfterRestart() throws Exception {
        String email = "restart-" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";
        String accessToken;

        // 1차 서버: 회원가입 → 로그인 → 로그아웃
        ConfigurableApplicationContext first = startServer();
        try {
            String baseUrl = baseUrl(first);

            assertThat(send(baseUrl, "POST", "/api/auth/signup", null, """
                    {"email":"%s","password":"password1234","name":"홍길동",
                     "nickname":"재시작","generation":14,"part":"BE"}
                    """.formatted(email)).statusCode()).isEqualTo(201);

            accessToken = login(baseUrl, email);

            assertThat(send(baseUrl, "GET", "/api/users/me", accessToken, null).statusCode())
                    .isEqualTo(200);
            assertThat(send(baseUrl, "POST", "/api/auth/logout", accessToken, null).statusCode())
                    .isEqualTo(204);
            assertThat(send(baseUrl, "GET", "/api/users/me", accessToken, null).statusCode())
                    .isEqualTo(401);
        } finally {
            first.close();
        }

        // 2차 서버: 새로 띄운 서버에서도 같은 토큰은 401이어야 해요
        ConfigurableApplicationContext second = startServer();
        try {
            String baseUrl = baseUrl(second);

            HttpResponse<String> me = send(baseUrl, "GET", "/api/users/me", accessToken, null);
            assertThat(me.statusCode()).isEqualTo(401);
            JsonNode error = objectMapper.readTree(me.body());
            assertThat(error.path("code").asText()).isEqualTo("COMMON-004");
            assertThat(error.path("path").asText()).isEqualTo("/api/users/me");

            // 새로 로그인한 토큰은 정상 동작해요
            String newToken = login(baseUrl, email);
            HttpResponse<String> newMe = send(baseUrl, "GET", "/api/users/me", newToken, null);
            assertThat(newMe.statusCode()).isEqualTo(200);
            assertThat(objectMapper.readTree(newMe.body()).path("data").path("email").asText())
                    .isEqualTo(email);
        } finally {
            cleanUp(second, email, accessToken);
            second.close();
        }
    }

    private ConfigurableApplicationContext startServer() {
        return new SpringApplicationBuilder(Backend14thApplication.class)
                .properties("server.port=0", "spring.jpa.show-sql=false")
                .run();
    }

    private String baseUrl(ConfigurableApplicationContext context) {
        return "http://localhost:" + context.getEnvironment().getProperty("local.server.port");
    }

    private String login(String baseUrl, String email) throws Exception {
        HttpResponse<String> response = send(baseUrl, "POST", "/api/auth/login", null, """
                {"email":"%s","password":"password1234"}
                """.formatted(email));
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body()).path("data").path("accessToken").asText();
    }

    private HttpResponse<String> send(
            String baseUrl, String method, String path, String token, String jsonBody
    ) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .method(method, jsonBody == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(jsonBody));
        if (jsonBody != null) {
            builder.header("Content-Type", "application/json");
        }
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    // 테스트가 DB에 남긴 회원과 무효화 행을 지워요.
    private void cleanUp(ConfigurableApplicationContext context, String email, String token) throws Exception {
        String tokenHash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))
        );
        RevokedTokenRepository revokedTokenRepository = context.getBean(RevokedTokenRepository.class);
        revokedTokenRepository.findAll().stream()
                .filter(revoked -> tokenHash.equals(revoked.getTokenHash()))
                .map(RevokedToken::getId)
                .forEach(revokedTokenRepository::deleteById);

        UserRepository userRepository = context.getBean(UserRepository.class);
        userRepository.findByEmail(email).ifPresent(userRepository::delete);
    }
}
