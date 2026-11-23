package com.cotato.backend14th.global.security;

import com.cotato.backend14th.domain.auth.controller.AuthController;
import com.cotato.backend14th.domain.auth.dto.LoginResponse;
import com.cotato.backend14th.domain.auth.dto.SignupResponse;
import com.cotato.backend14th.domain.auth.service.AuthService;
import com.cotato.backend14th.domain.auth.service.TokenRevocationService;
import com.cotato.backend14th.domain.user.controller.UserController;
import com.cotato.backend14th.domain.user.dto.UserResponse;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.service.UserService;
import com.cotato.backend14th.global.config.CorsConfig;
import com.cotato.backend14th.global.config.JwtConfig;
import com.cotato.backend14th.global.config.SecurityConfig;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Security 필터와 JWT 검증, 401 응답 형식을 DB 없이 확인하는 테스트예요.
@WebMvcTest(controllers = {UserController.class, AuthController.class})
@Import({SecurityConfig.class, JwtConfig.class, CorsConfig.class,
        JwtAuthenticationEntryPoint.class, RevokedTokenValidator.class})
@TestPropertySource(properties = "JWT_SECRET=test-only-jwt-secret-key-at-least-32-bytes!!")
class JwtSecurityTest {

    private static final String SIGNUP_BODY = """
            {"email":"user@gmail.com","password":"password1234","name":"홍길동",
             "nickname":"길동이","generation":14,"part":"BE"}
            """;
    private static final String LOGIN_BODY = """
            {"email":"user@gmail.com","password":"password1234"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    // 기본값(false)이면 무효화되지 않은 토큰으로 취급해요.
    @MockitoBean
    private TokenRevocationService tokenRevocationService;

    // @EnableJpaAuditing 때문에 WebMvcTest에서 필요한 빈이에요.
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("유효한 JWT로 내 정보를 조회하면 200과 회원 정보를 반환해요")
    void getMyInfo_validToken() throws Exception {
        given(userService.getMyInfo(1L)).willReturn(new UserResponse(
                1L, "user@gmail.com", "홍길동", "길동이", 14, Part.BE
        ));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + createToken("1", Instant.now().plusSeconds(3600))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.email").value("user@gmail.com"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.nickname").value("길동이"))
                .andExpect(jsonPath("$.data.generation").value(14))
                .andExpect(jsonPath("$.data.part").value("BE"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("토큰 없이 내 정보를 조회하면 401 ErrorResponse를 반환해요")
    void getMyInfo_noToken() throws Exception {
        expectUnauthorized(mockMvc.perform(get("/api/users/me")), "GET", "/api/users/me");
    }

    @Test
    @DisplayName("형식이 잘못된 토큰이면 401을 반환해요")
    void getMyInfo_malformedToken() throws Exception {
        expectUnauthorized(
                mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not.a.jwt")),
                "GET", "/api/users/me"
        );
    }

    @Test
    @DisplayName("다른 키로 서명한 토큰이면 401을 반환해요")
    void getMyInfo_wrongSignature() throws Exception {
        JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
                "another-secret-key-that-is-32-bytes-long!!".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        )));
        String forged = encode(otherEncoder, "1", Instant.now().plusSeconds(3600));

        expectUnauthorized(
                mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + forged)),
                "GET", "/api/users/me"
        );
    }

    @Test
    @DisplayName("만료된 토큰이면 401을 반환해요")
    void getMyInfo_expiredToken() throws Exception {
        String expired = createToken("1", Instant.now().minusSeconds(3600));

        expectUnauthorized(
                mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + expired)),
                "GET", "/api/users/me"
        );
    }

    @Test
    @DisplayName("토큰은 유효하지만 회원이 없으면 401을 반환해요")
    void getMyInfo_userNotFound() throws Exception {
        given(userService.getMyInfo(999L))
                .willThrow(new Backend14thException(ErrorCode.UNAUTHORIZED));

        expectUnauthorized(
                mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + createToken("999", Instant.now().plusSeconds(3600)))),
                "GET", "/api/users/me"
        );
    }

    @Test
    @DisplayName("회원가입은 토큰 없이 호출할 수 있어요")
    void signup_isPublic() throws Exception {
        given(authService.signup(any())).willReturn(new SignupResponse(1L));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIGNUP_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    @DisplayName("만료된 토큰이 헤더에 남아 있어도 로그인할 수 있어요")
    void login_isPublic_evenWithExpiredToken() throws Exception {
        given(authService.login(any())).willReturn(new LoginResponse("token", "Bearer"));

        mockMvc.perform(post("/api/auth/login")
                        .header("Authorization", "Bearer " + createToken("1", Instant.now().minusSeconds(3600)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 기존처럼 401 INVALID_CREDENTIALS를 반환해요")
    void login_invalidCredentials() throws Exception {
        given(authService.login(any()))
                .willThrow(new Backend14thException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER-002"));
    }

    @Test
    @DisplayName("로그인이 필요한 API를 토큰 없이 호출하면 401을 반환해요")
    void protectedApi_noToken() throws Exception {
        expectUnauthorized(
                mockMvc.perform(post("/api/study-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")),
                "POST", "/api/study-posts"
        );
        expectUnauthorized(
                mockMvc.perform(post("/api/auth/logout")),
                "POST", "/api/auth/logout"
        );
    }

    @Test
    @DisplayName("모집글·댓글 조회와 Swagger는 토큰 없이 401이 나지 않아요")
    void publicGetApis_notUnauthorized() throws Exception {
        // 아직 컨트롤러가 없어서 404/500이 나올 수 있지만 Security에서 막히지 않는지만 확인해요.
        for (String path : new String[]{
                "/api/study-posts",
                "/api/study-posts/1",
                "/api/study-posts/1/comments",
                "/v3/api-docs",
                "/swagger-ui/index.html"
        }) {
            int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertThat(status).as(path).isNotEqualTo(401);
        }
    }

    @Test
    @DisplayName("유효한 JWT로 로그아웃하면 본문 없이 204를 반환해요")
    void logout_validToken() throws Exception {
        String token = createToken("1", Instant.now().plusSeconds(3600));

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        then(authService).should().logout(argThat(jwt -> token.equals(jwt.getTokenValue())));
    }

    @Test
    @DisplayName("무효화된 토큰으로 요청하면 401 ErrorResponse를 반환해요")
    void revokedToken_unauthorized() throws Exception {
        String token = createToken("1", Instant.now().plusSeconds(3600));
        given(tokenRevocationService.isRevoked(token)).willReturn(true);

        expectUnauthorized(
                mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token)),
                "GET", "/api/users/me"
        );
        expectUnauthorized(
                mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token)),
                "POST", "/api/auth/logout"
        );
        then(authService).should(never()).logout(any());
    }

    @Test
    @DisplayName("기존 /books, /hello API는 토큰 없이 401이 나지 않아요")
    void legacyApis_notUnauthorized() throws Exception {
        // 이 슬라이스 테스트에는 Book/Hello 컨트롤러가 없어서 Security에서 막히지 않는지만 확인해요.
        assertThat(mockMvc.perform(get("/books/1")).andReturn().getResponse().getStatus())
                .as("GET /books/1").isNotEqualTo(401);
        assertThat(mockMvc.perform(get("/hello")).andReturn().getResponse().getStatus())
                .as("GET /hello").isNotEqualTo(401);
        assertThat(mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"author\":\"a\"}"))
                .andReturn().getResponse().getStatus())
                .as("POST /books").isNotEqualTo(401);
    }

    @Test
    @DisplayName("기존 API와 비슷하지만 매핑되지 않은 경로는 여전히 인증이 필요해요")
    void legacyLikeApis_stillProtected() throws Exception {
        expectUnauthorized(mockMvc.perform(delete("/books/1")), "DELETE", "/books/1");
        expectUnauthorized(mockMvc.perform(post("/hello")), "POST", "/hello");
    }

    private void expectUnauthorized(ResultActions result, String method, String path) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("COMMON-004"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.method").value(method))
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private String createToken(String subject, Instant expiresAt) {
        return encode(jwtEncoder, subject, expiresAt);
    }

    private String encode(JwtEncoder encoder, String subject, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
