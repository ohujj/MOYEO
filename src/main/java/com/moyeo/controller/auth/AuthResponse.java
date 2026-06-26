package com.moyeo.controller.auth;

import com.moyeo.service.member.AuthenticatedMember;

public record AuthResponse(
        String accessToken,
        String tokenType,
        AuthUserResponse user
) {

    private static final String BEARER = "Bearer";

    public static AuthResponse of(String accessToken, AuthenticatedMember member) {
        return new AuthResponse(accessToken, BEARER, AuthUserResponse.from(member));
    }
}
