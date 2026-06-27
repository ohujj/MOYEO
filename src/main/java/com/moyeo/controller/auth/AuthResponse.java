package com.moyeo.controller.auth;

import com.moyeo.service.member.AuthenticatedMember;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 성공 응답")
public record AuthResponse(
        @Schema(description = "Access Token", example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "인증된 사용자 정보")
        AuthUserResponse user
) {

    private static final String BEARER = "Bearer";

    public static AuthResponse of(String accessToken, AuthenticatedMember member) {
        return new AuthResponse(accessToken, BEARER, AuthUserResponse.from(member));
    }
}
