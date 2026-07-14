package com.moyeo.development;

import com.moyeo.controller.auth.AuthResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "개발 테스트 계정 2개의 Access Token 발급 응답")
record DevAuthTokensResponse(
        @Schema(description = "테스트 사용자 1의 인증 응답")
        AuthResponse userOne,

        @Schema(description = "테스트 사용자 2의 인증 응답")
        AuthResponse userTwo
) {
}
