// ResponseUtils.java
package com.cotato.backend14th.global.common.response;

import org.springframework.http.ResponseEntity;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ResponseUtils {

    public <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    public <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity
                .status(201)
                .body(ApiResponse.created(data));
    }
}