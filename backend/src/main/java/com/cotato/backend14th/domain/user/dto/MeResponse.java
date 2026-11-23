package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;

public record MeResponse(Long userId, String email, String name, String nickname, int generation, Part part) {
}
