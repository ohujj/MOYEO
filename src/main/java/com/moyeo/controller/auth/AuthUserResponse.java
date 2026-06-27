package com.moyeo.controller.auth;

import com.moyeo.service.member.AuthenticatedMember;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 사용자 응답")
public record AuthUserResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long id,

        @Schema(description = "사용자 기본 닉네임", example = "moyeo1")
        String nickname
) {

    public static AuthUserResponse from(AuthenticatedMember member) {
        return new AuthUserResponse(member.userId(), member.nickname());
    }
}
