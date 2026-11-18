package com.cotato.backend14th.global.security;

import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BearerToken {

    private final String PREFIX = "Bearer ";

    // Authorization 헤더에서 "Bearer " 뒤의 토큰 문자열을 꺼내요. 없으면 null이에요.
    public String resolve(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length()).trim();
        }
        return null;
    }
}
