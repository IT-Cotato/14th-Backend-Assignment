package com.cotato.backend14th.domain.user.controller;

import com.cotato.backend14th.JwtTestConfiguration;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(JwtTestConfiguration.class)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:signup")
@AutoConfigureMockMvc
class SignupControllerTests {

    private static final String SIGNUP_PATH = "/api/auth/signup";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void signsUpWithoutAuthenticationAndStoresBcryptHash() throws Exception {
        ObjectNode request = validRequest("new-user@example.com");
        String response = signup(request.toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        long userId = objectMapper.readTree(response).at("/data/userId").longValue();
        assertThat(userId).isPositive();
        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getEmail()).isEqualTo("new-user@example.com");
        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.getNickname()).isEqualTo("길동이");
        assertThat(user.getGeneration()).isEqualTo(14);
        assertThat(user.getPart()).isEqualTo(Part.BE);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getPassword()).isNotEqualTo(request.get("password").asText());
        assertThat(new BCryptPasswordEncoder().matches(request.get("password").asText(), user.getPassword())).isTrue();
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        String request = validRequest("duplicate@example.com").toString();
        signup(request).andExpect(status().isCreated());

        signup(request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER-001"));
    }

    @ParameterizedTest
    @CsvSource({
            "email, '\"invalid\"'",
            "password, '\"short\"'",
            "name, '\"\"'",
            "nickname, '\"a\"'",
            "nickname, '\"12345678901\"'",
            "generation, 0",
            "generation, 1.5",
            "generation, '\"14\"'",
            "part, '\"UNKNOWN\"'",
            "part, 2",
            "part, null"
    })
    void rejectsInvalidFields(String field, String jsonValue) throws Exception {
        ObjectNode request = validRequest("invalid@example.com");
        request.set(field, objectMapper.readTree(jsonValue));
        expectInvalidInput(request.toString());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        expectInvalidInput("{");
    }

    @Test
    void rejectsPasswordsPastBcryptByteLimit() throws Exception {
        ObjectNode request = validRequest("invalid@example.com");
        request.put("password", "가".repeat(25));
        expectInvalidInput(request.toString());
    }

    private ObjectNode validRequest(String email) {
        return objectMapper.createObjectNode()
                .put("email", email)
                .put("password", "sample-password")
                .put("name", "홍길동")
                .put("nickname", "길동이")
                .put("generation", 14)
                .put("part", "BE");
    }

    private ResultActions signup(String body) throws Exception {
        return mockMvc.perform(post(SIGNUP_PATH).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void expectInvalidInput(String body) throws Exception {
        signup(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-003"))
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.path").value(SIGNUP_PATH));
        assertThat(userRepository.existsByEmail("invalid@example.com")).isFalse();
    }
}
