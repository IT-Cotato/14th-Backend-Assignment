package com.cotato.backend14th.global.common.response;

import lombok.experimental.UtilityClass;
import org.springframework.http.ResponseEntity;

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
