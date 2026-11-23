package com.cotato.backend14th.domain.auth.dto;

import com.cotato.backend14th.domain.user.entity.Part;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 8)
    private String password;

    @NotBlank @Size(max = 50)
    private String name;

    @NotBlank @Size(min = 2, max = 10)
    private String nickname;

    @NotNull @Min(1)
    private Integer generation;

    @NotNull
    private Part part;

}
