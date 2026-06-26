package com.moyeo.controller.auth;

import com.moyeo.service.member.AuthenticatedMember;

public record AuthUserResponse(
        Long id,
        String nickname
) {

    public static AuthUserResponse from(AuthenticatedMember member) {
        return new AuthUserResponse(member.userId(), member.nickname());
    }
}
