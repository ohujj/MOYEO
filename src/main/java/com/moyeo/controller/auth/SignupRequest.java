package com.moyeo.controller.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "일반 회원가입 요청")
public record SignupRequest(
        @Schema(
                description = "로그인 ID",
                example = "moyeo1",
                minLength = 4,
                maxLength = 50,
                pattern = "^[a-zA-Z0-9._-]+$"
        )
        @NotBlank
        @Size(min = 4, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
        String loginId,

        @Schema(description = "비밀번호", example = "moyeopasswd", minLength = 8, maxLength = 72)
        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @Schema(description = "회원 기본 닉네임. 전역 고유값이 아니며, 모임 안 표시 닉네임 중복은 MeetingParticipant 기준으로 처리합니다.", example = "moyeo1", minLength = 1, maxLength = 30)
        @NotBlank
        @Size(min = 1, max = 30)
        String nickname
) {
}
