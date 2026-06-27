package com.moyeo.controller.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "일반 로그인 요청")
public record LoginRequest(
        @Schema(description = "로그인 ID", example = "moyeo1", minLength = 4, maxLength = 50)
        @NotBlank
        @Size(min = 4, max = 50)
        String loginId,

        @Schema(description = "비밀번호", example = "moyeopasswd", minLength = 8, maxLength = 72)
        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}
