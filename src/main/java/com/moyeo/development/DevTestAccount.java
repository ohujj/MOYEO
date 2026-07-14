package com.moyeo.development;

import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.MemberAuthService;

enum DevTestAccount {

    USER_ONE("dev-user-1", "dev-password-1", "개발 사용자 1"),
    USER_TWO("dev-user-2", "dev-password-2", "개발 사용자 2");

    private final String loginId;
    private final String password;
    private final String nickname;

    DevTestAccount(String loginId, String password, String nickname) {
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
    }

    void createIfMissing(MemberAuthService memberAuthService) {
        memberAuthService.registerLocalIfMissing(loginId, password, nickname);
    }

    AuthenticatedMember login(MemberAuthService memberAuthService) {
        return memberAuthService.loginLocal(loginId, password);
    }
}
