package com.cotato.backend14th.global.common.response;

import com.cotato.backend14th.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

@Isolated
class ResponseTimestampTests {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    void serializesSuccessAndErrorTimestampsInSeoulWhenJvmUsesUtc() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            LocalDateTime before = LocalDateTime.now(SEOUL).truncatedTo(ChronoUnit.SECONDS);
            ApiResponse<String> success = ApiResponse.success("ok");
            ErrorResponse error = ErrorResponse.of(
                    ErrorCode.INVALID_INPUT, new MockHttpServletRequest("POST", "/api/auth/signup"));
            LocalDateTime after = LocalDateTime.now(SEOUL).truncatedTo(ChronoUnit.SECONDS);

            for (Object response : new Object[]{success, error}) {
                String timestamp = mapper.valueToTree(response).path("timestamp").asText();
                LocalDateTime parsed = LocalDateTime.parse(timestamp, FORMAT);
                assertThat(timestamp).isEqualTo(parsed.format(FORMAT));
                assertThat(parsed).isBetween(before, after);
            }
        } finally {
            TimeZone.setDefault(previous);
        }
    }
}
