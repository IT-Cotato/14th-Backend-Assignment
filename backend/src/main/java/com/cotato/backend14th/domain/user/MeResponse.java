package com.cotato.backend14th.domain.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MeResponse {
    private Long userId;
    private String email;
    private String name;
    private String nickname;
    private Integer generation;
    private Part part;
}