package com.cotato.backend14th.domain.auth.controller;

import com.cotato.backend14th.domain.auth.service.AuthService;
import com.cotato.backend14th.domain.auth.service.TokenRevocationService;
import com.cotato.backend14th.global.config.CorsConfig;
import com.cotato.backend14th.global.config.JwtConfig;
import com.cotato.backend14th.global.config.SecurityConfig;
import com.cotato.backend14th.global.security.JwtAuthenticationEntryPoint;
import com.cotato.backend14th.global.security.RevokedTokenValidator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 회원가입·로그인 요청값이 잘못되면 서비스까지 가지 않고 400 COMMON-003을 반환하는지 확인해요.
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtConfig.class, CorsConfig.class,
        JwtAuthenticationEntryPoint.class, RevokedTokenValidator.class})
@TestPropertySource(properties = "JWT_SECRET=test-only-jwt-secret-key-at-least-32-bytes!!")
class AuthValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private TokenRevocationService tokenRevocationService;

    // @EnableJpaAuditing 때문에 WebMvcTest에서 필요한 빈이에요.
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("회원가입 요청값이 잘못되면 400 COMMON-003이에요")
    @ValueSource(strings = {
            // 이메일 형식 오류
            """
            {"email":"not-email","password":"password1234","name":"홍길동","nickname":"길동이","generation":14,"part":"BE"}""",
            // 비밀번호 8자 미만
            """
            {"email":"user@gmail.com","password":"short","name":"홍길동","nickname":"길동이","generation":14,"part":"BE"}""",
            // 이름 공백
            """
            {"email":"user@gmail.com","password":"password1234","name":" ","nickname":"길동이","generation":14,"part":"BE"}""",
            // 닉네임 10자 초과
            """
            {"email":"user@gmail.com","password":"password1234","name":"홍길동","nickname":"닉네임이열한글자입니다","generation":14,"part":"BE"}""",
            // 기수 0
            """
            {"email":"user@gmail.com","password":"password1234","name":"홍길동","nickname":"길동이","generation":0,"part":"BE"}""",
            // 파트 누락
            """
            {"email":"user@gmail.com","password":"password1234","name":"홍길동","nickname":"길동이","generation":14}""",
            // 정의되지 않은 파트
            """
            {"email":"user@gmail.com","password":"password1234","name":"홍길동","nickname":"길동이","generation":14,"part":"XX"}""",
            // JSON 형식 오류
            """
            {"email":"""
    })
    void signup_invalidInput(String body) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-003"))
                .andExpect(jsonPath("$.path").value("/api/auth/signup"));

        then(authService).should(never()).signup(any());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("로그인 요청값이 잘못되면 400 COMMON-003이에요")
    @ValueSource(strings = {
            """
            {"email":"","password":"password1234"}""",
            """
            {"email":"not-email","password":"password1234"}""",
            """
            {"email":"user@gmail.com","password":""}"""
    })
    void login_invalidInput(String body) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-003"));

        then(authService).should(never()).login(any());
    }
}
