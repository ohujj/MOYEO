package com.moyeo.development;

import com.moyeo.controller.auth.AuthResponse;
import com.moyeo.global.security.JwtTokenProvider;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.MemberAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local", "dev"})
@RequestMapping("/api/auth/dev")
@Tag(name = "Development Auth", description = "local 및 dev 프로필에서만 제공되는 테스트 계정 인증 API")
class DevAuthController {

    private final MemberAuthService memberAuthService;
    private final JwtTokenProvider jwtTokenProvider;

    DevAuthController(MemberAuthService memberAuthService, JwtTokenProvider jwtTokenProvider) {
        this.memberAuthService = memberAuthService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/tokens")
    @Operation(
            summary = "개발 테스트 계정 Access Token 일괄 발급",
            description = "local 및 dev 프로필에서만 노출됩니다. 요청 본문 없이 테스트 사용자 1과 2의 Access Token을 함께 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "두 테스트 계정의 Access Token 발급 성공")
    DevAuthTokensResponse issueTokens() {
        return new DevAuthTokensResponse(issueToken(DevTestAccount.USER_ONE), issueToken(DevTestAccount.USER_TWO));
    }

    private AuthResponse issueToken(DevTestAccount account) {
        AuthenticatedMember member = account.login(memberAuthService);
        return AuthResponse.of(jwtTokenProvider.createAccessToken(member), member);
    }
}
