package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos() {}

    public record Signup(@NotBlank @Email @Size(max = 254) String email,
                         @NotBlank @Size(min = 8, max = 72) String password,
                         @NotBlank @Size(max = 100) String name,
                         @NotBlank @Size(min = 2, max = 10) String nickname,
                         @NotNull @Positive Integer generation,
                         @NotNull Part part) {}
    public record Login(String email, String password) {}
    public record UserId(long userId) {}
    public record Token(String accessToken, String tokenType) {}
    public record Profile(long userId, String email, String name, String nickname, int generation, Part part) {
        public static Profile from(User user) {
            return new Profile(user.getId(), user.getEmail(), user.getName(), user.getNickname(),
                    user.getGeneration(), user.getPart());
        }
    }
    public record Author(String nickname, int generation, Part part) {
        public static Author from(User user) {
            return new Author(user.getNickname(), user.getGeneration(), user.getPart());
        }
    }
}
