package com.moyeo.global.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.service.member.AuthenticatedMember;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-test-secret-test-secret";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-27T00:00:00Z"), ZoneOffset.UTC);
    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            new JwtProperties(SECRET, 3600),
            OBJECT_MAPPER,
            clock
    );

    @Test
    void createAndParseAccessToken() {
        String token = jwtTokenProvider.createAccessToken(new AuthenticatedMember(1L, "moyeo", true));

        JwtClaims claims = jwtTokenProvider.parse(token);

        assertThat(claims.userId()).isEqualTo(1L);
        assertThat(claims.nickname()).isEqualTo("moyeo");
        assertThat(claims.role()).isEqualTo("USER");
    }

    @Test
    void parseRejectsTamperedToken() {
        String token = jwtTokenProvider.createAccessToken(new AuthenticatedMember(1L, "moyeo", true));
        String tamperedToken = token.substring(0, token.length() - 1) + "x";

        assertThatThrownBy(() -> jwtTokenProvider.parse(tamperedToken))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseRejectsExpiredToken() {
        String token = jwtTokenProvider.createAccessToken(new AuthenticatedMember(1L, "moyeo", true));
        JwtTokenProvider expiredParser = new JwtTokenProvider(
                new JwtProperties(SECRET, 3600),
                OBJECT_MAPPER,
                Clock.fixed(Instant.parse("2026-06-27T01:00:00Z"), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> expiredParser.parse(token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseRejectsMalformedToken() {
        assertThatThrownBy(() -> jwtTokenProvider.parse("header..signature"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseRejectsUnsupportedAlgorithmHeader() {
        String token = createToken(
                Map.of("typ", "JWT", "alg", "none"),
                validPayload()
        );

        assertThatThrownBy(() -> jwtTokenProvider.parse(token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseRejectsMissingRequiredClaim() {
        Map<String, Object> payload = validPayload();
        payload.remove("nickname");
        String token = createToken(validHeader(), payload);

        assertThatThrownBy(() -> jwtTokenProvider.parse(token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseRejectsInvalidSubjectClaim() {
        Map<String, Object> payload = validPayload();
        payload.put("sub", "not-number");
        String token = createToken(validHeader(), payload);

        assertThatThrownBy(() -> jwtTokenProvider.parse(token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void propertiesRejectBlankSecret() {
        assertThatThrownBy(() -> new JwtProperties(" ", 3600))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void propertiesRejectNonPositiveAccessTokenValiditySeconds() {
        assertThatThrownBy(() -> new JwtProperties(SECRET, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Map<String, Object> validHeader() {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("typ", "JWT");
        header.put("alg", "HS256");
        return header;
    }

    private Map<String, Object> validPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", "1");
        payload.put("nickname", "moyeo");
        payload.put("role", "USER");
        payload.put("iat", Instant.now(clock).getEpochSecond());
        payload.put("exp", Instant.now(clock).plusSeconds(3600).getEpochSecond());
        return payload;
    }

    private String createToken(Map<String, Object> header, Map<String, Object> payload) {
        String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = OBJECT_MAPPER.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
