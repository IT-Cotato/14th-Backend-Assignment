// ApiResponse.java
package com.cotato.backend14th.global.common.response;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // data가 null이면 응답에서 아예 필드를 빼줘요
public class ApiResponse<T> {

    private final boolean success;
    private final int status;
    private final T data;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private final LocalDateTime timestamp = LocalDateTime.now();

    private ApiResponse(boolean success, int status, T data) {
        this.success = success;
        this.status = status;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, HttpStatus.CREATED.value(), data);
    }
}