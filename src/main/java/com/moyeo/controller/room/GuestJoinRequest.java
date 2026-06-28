package com.moyeo.controller.room;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "게스트 참여 요청")
public record GuestJoinRequest(
        @Schema(description = "모임 안에서 사용할 표시 닉네임", example = "guest1", minLength = 1, maxLength = 30)
        @NotBlank
        @Size(min = 1, max = 30)
        String nickname,

        @Schema(description = "게스트 참여 비밀번호", example = "guestpass123", minLength = 8, maxLength = 72)
        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}
