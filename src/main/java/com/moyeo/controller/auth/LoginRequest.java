package com.moyeo.controller.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        @Size(min = 4, max = 50)
        String loginId,

        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}
