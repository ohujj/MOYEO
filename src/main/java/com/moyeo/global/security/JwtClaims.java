package com.moyeo.global.security;

public record JwtClaims(
        Long userId,
        String nickname,
        String role
) {
}
