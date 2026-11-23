package com.cotato.backend14th.domain.user;

import com.cotato.backend14th.global.common.response.ApiResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User API")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Operation(summary = "내 정보")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new Backend14thException(ErrorCode.UNAUTHORIZED);
        }
        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));
        return ResponseUtils.ok(new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getGeneration(),
                user.getPart()
        ));
    }
}