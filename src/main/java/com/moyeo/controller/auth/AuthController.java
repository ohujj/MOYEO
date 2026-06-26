package com.moyeo.controller.auth;

import com.moyeo.global.security.JwtTokenProvider;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.MemberAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberAuthService memberAuthService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(MemberAuthService memberAuthService, JwtTokenProvider jwtTokenProvider) {
        this.memberAuthService = memberAuthService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        AuthenticatedMember member = memberAuthService.registerLocal(
                request.loginId(),
                request.password(),
                request.nickname()
        );
        return AuthResponse.of(jwtTokenProvider.createAccessToken(member), member);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        AuthenticatedMember member = memberAuthService.loginLocal(request.loginId(), request.password());
        return AuthResponse.of(jwtTokenProvider.createAccessToken(member), member);
    }
}
