package com.cotato.backend14th.global.exception;

import com.cotato.backend14th.domain.auth.controller.AuthController;
import com.cotato.backend14th.domain.auth.service.AuthService;
import com.cotato.backend14th.domain.auth.service.TokenRevocationService;
import com.cotato.backend14th.domain.user.controller.UserController;
import com.cotato.backend14th.domain.user.service.UserService;
import com.cotato.backend14th.global.config.CorsConfig;
import com.cotato.backend14th.global.config.JwtConfig;
import com.cotato.backend14th.global.config.SecurityConfig;
import com.cotato.backend14th.global.security.JwtAuthenticationEntryPoint;
import com.cotato.backend14th.global.security.RevokedTokenValidator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 없는 URL(404)과 지원하지 않는 메서드(405)가 500으로 바뀌지 않는지 확인하는 테스트예요.
@WebMvcTest(controllers = {UserController.class, AuthController.class})
@Import({SecurityConfig.class, JwtConfig.class, CorsConfig.class,
        JwtAuthenticationEntryPoint.class, RevokedTokenValidator.class})
@TestPropertySource(properties = "JWT_SECRET=test-only-jwt-secret-key-at-least-32-bytes!!")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private TokenRevocationService tokenRevocationService;

    // @EnableJpaAuditing 때문에 WebMvcTest에서 필요한 빈이에요.
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("로그인한 사용자가 없는 API URL을 요청하면 404 COMMON-005를 반환해요")
    void unknownUrl_returns404() throws Exception {
        mockMvc.perform(get("/api/not-exist").header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMMON-005"))
                .andExpect(jsonPath("$.message").value("요청한 API를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.path").value("/api/not-exist"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드로 요청하면 405 COMMON-006과 Allow 헤더를 반환해요")
    void unsupportedMethod_returns405() throws Exception {
        mockMvc.perform(delete("/api/users/me").header("Authorization", bearer()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.code").value("COMMON-006"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 HTTP 메서드입니다."))
                .andExpect(jsonPath("$.method").value("DELETE"))
                .andExpect(jsonPath("$.path").value("/api/users/me"));
    }

    @Test
    @DisplayName("공개 API 경로에 지원하지 않는 메서드로 요청해도 405를 반환해요")
    void unsupportedMethodOnAuthPath_returns405() throws Exception {
        mockMvc.perform(put("/api/auth/logout").header("Authorization", bearer()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("COMMON-006"));
    }

    @Test
    @DisplayName("토큰 없이 없는 URL을 요청하면 Security가 먼저 401을 반환해요")
    void unknownUrlWithoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/not-exist"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON-004"));
    }

    private String bearer() {
        Instant expiresAt = Instant.now().plusSeconds(3600);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("1")
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
