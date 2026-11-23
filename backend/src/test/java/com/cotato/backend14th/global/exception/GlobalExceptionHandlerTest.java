package com.cotato.backend14th.global.exception;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("없는 주소로 요청하면 404 RESOURCE_NOT_FOUND를 내려준다")
    void notFound() throws Exception {
        mockMvc.perform(get("/no-such-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON-005"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.path").value("/no-such-path"));
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드로 요청하면 405 METHOD_NOT_ALLOWED와 Allow 헤더를 내려준다")
    void methodNotAllowed() throws Exception {
        mockMvc.perform(post("/test"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.code").value("COMMON-006"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @RestController
    static class TestController {

        @GetMapping("/test")
        String test() {
            return "ok";
        }
    }
}
