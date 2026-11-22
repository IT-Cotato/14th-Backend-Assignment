package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.global.common.request.TextDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.io.IOException;

public record SignupRequest(
        @JsonDeserialize(using = TextDeserializer.class) @NotBlank @Email @Size(max = 255) String email,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @JsonDeserialize(using = TextDeserializer.class) @NotNull @Size(min = 8) String password,
        @JsonDeserialize(using = TextDeserializer.class) @NotBlank @Size(max = 100) String name,
        @JsonDeserialize(using = TextDeserializer.class) @NotBlank @Size(min = 2, max = 10) String nickname,
        @JsonDeserialize(using = GenerationDeserializer.class) @NotNull @Positive Integer generation,
        @JsonDeserialize(using = PartDeserializer.class) @NotNull Part part
) {

    static class GenerationDeserializer extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return context.reportInputMismatch(Integer.class, "generation must be an integer");
            }
            return parser.getIntValue();
        }
    }

    static class PartDeserializer extends JsonDeserializer<Part> {
        @Override
        public Part deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return context.reportInputMismatch(Part.class, "part must be PM, DE, BE or FE");
            }

            try {
                return Part.valueOf(parser.getText());
            } catch (IllegalArgumentException e) {
                return context.reportInputMismatch(Part.class, "part must be PM, DE, BE or FE");
            }
        }
    }
}
