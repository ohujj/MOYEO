package com.moyeo.controller.auth;

import com.moyeo.global.security.CurrentMember;
import com.moyeo.global.security.JwtTokenProvider;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.MemberAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "회원가입, 로그인, 현재 사용자 조회 API")
public class AuthController {

    private final MemberAuthService memberAuthService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(MemberAuthService memberAuthService, JwtTokenProvider jwtTokenProvider) {
        this.memberAuthService = memberAuthService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "일반 회원가입",
            description = "로그인 ID, 비밀번호, 닉네임으로 회원가입하고 Access Token을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON_VALIDATION_FAILED",
                              "status": 400
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 사용 중인 로그인 ID",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "DUPLICATE_LOGIN_ID",
                              "status": 409
                            }
                            """))
            )
    })
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        AuthenticatedMember member = memberAuthService.registerLocal(
                request.loginId(),
                request.password(),
                request.nickname()
        );
        return AuthResponse.of(jwtTokenProvider.createAccessToken(member), member);
    }

    @PostMapping("/login")
    @Operation(
            summary = "일반 로그인",
            description = "로그인 ID와 비밀번호로 로그인하고 Access Token을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON_VALIDATION_FAILED",
                              "status": 400
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "로그인 ID 또는 비밀번호 불일치",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "INVALID_LOGIN_CREDENTIALS",
                              "status": 401
                            }
                            """))
            )
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        AuthenticatedMember member = memberAuthService.loginLocal(request.loginId(), request.password());
        return AuthResponse.of(jwtTokenProvider.createAccessToken(member), member);
    }

    @GetMapping("/me")
    @Operation(
            summary = "현재 사용자 조회",
            description = "`Authorization: Bearer {accessToken}` 헤더로 현재 로그인 사용자를 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "현재 사용자 조회 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token 없음, 만료 또는 유효하지 않음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "AUTHENTICATION_REQUIRED",
                              "status": 401
                            }
                            """))
            )
    })
    public AuthUserResponse me(@Parameter(hidden = true) @CurrentMember AuthenticatedMember member) {
        return AuthUserResponse.from(member);
    }
}
