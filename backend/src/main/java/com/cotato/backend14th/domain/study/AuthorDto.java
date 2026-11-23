package com.cotato.backend14th.domain.study;

import com.cotato.backend14th.domain.user.Part;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthorDto {
    private String nickname;
    private Integer generation;
    private Part part;
}
