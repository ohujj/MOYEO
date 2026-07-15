package com.moyeo.service.meeting;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class InviteCodeGenerator {

    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 10;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int index = 0; index < CODE_LENGTH; index++) {
            builder.append(CODE_CHARS[secureRandom.nextInt(CODE_CHARS.length)]);
        }
        return builder.toString();
    }
}
