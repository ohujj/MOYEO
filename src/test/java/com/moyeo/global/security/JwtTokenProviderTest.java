package com.moyeo.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.service.member.AuthenticatedMember;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-27T00:00:00Z"), ZoneOffset.UTC);
    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            new JwtProperties("test-secret-test-secret-test-secret", 3600),
            new ObjectMapper(),
            clock
    );

    @Test
    void createAndParseAccessToken() {
        String token = jwtTokenProvider.createAccessToken(new AuthenticatedMember(1L, "모여", true));

        JwtClaims claims = jwtTokenProvider.parse(token);

        assertThat(claims.userId()).isEqualTo(1L);
        assertThat(claims.nickname()).isEqualTo("모여");
        assertThat(claims.role()).isEqualTo("USER");
    }

    @Test
    void parseRejectsTamperedToken() {
        String token = jwtTokenProvider.createAccessToken(new AuthenticatedMember(1L, "모여", true));
        String tamperedToken = token.substring(0, token.length() - 1) + "x";

        assertThatThrownBy(() -> jwtTokenProvider.parse(tamperedToken))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
