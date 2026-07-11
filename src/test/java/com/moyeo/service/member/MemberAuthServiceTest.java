package com.moyeo.service.member;

import com.moyeo.domain.member.AuthProvider;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.repository.member.LoginAccountRepository;
import com.moyeo.repository.member.SocialAccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({MemberAuthService.class, TestPasswordEncoderConfig.class})
class MemberAuthServiceTest {

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private LoginAccountRepository loginAccountRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void registerLocalCreatesUserAndHashedPassword() {
        AuthenticatedMember member = memberAuthService.registerLocal("moyeo", "password123!", "모여");

        assertThat(member.userId()).isNotNull();
        assertThat(member.nickname()).isEqualTo("모여");
        assertThat(member.registered()).isTrue();

        var loginAccount = loginAccountRepository.findByLoginId("moyeo").orElseThrow();
        assertThat(loginAccount.getPasswordHash()).isNotEqualTo("password123!");
        assertThat(passwordEncoder.matches("password123!", loginAccount.getPasswordHash())).isTrue();
    }

    @Test
    void registerLocalRejectsDuplicatedLoginId() {
        memberAuthService.registerLocal("moyeo", "password123!", "모여");

        assertThatThrownBy(() -> memberAuthService.registerLocal("moyeo", "password123!", "중복"))
                .isInstanceOfSatisfying(MoyeoException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.DUPLICATE_LOGIN_ID)
                );
    }

    @Test
    void loginLocalReturnsExistingUser() {
        AuthenticatedMember registered = memberAuthService.registerLocal("moyeo", "password123!", "모여");

        AuthenticatedMember loggedIn = memberAuthService.loginLocal("moyeo", "password123!");

        assertThat(loggedIn.userId()).isEqualTo(registered.userId());
        assertThat(loggedIn.registered()).isFalse();
    }

    @Test
    void loginLocalRejectsWrongPassword() {
        memberAuthService.registerLocal("moyeo", "password123!", "모여");

        assertThatThrownBy(() -> memberAuthService.loginLocal("moyeo", "wrong-password"))
                .isInstanceOfSatisfying(MoyeoException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIALS)
                );
    }

    @Test
    void loginLocalRejectsSoftDeletedUser() {
        AuthenticatedMember member = memberAuthService.registerLocal("deleted", "password123!", "deleted");
        softDeleteUser(member.userId());

        assertThatThrownBy(() -> memberAuthService.loginLocal("deleted", "password123!"))
                .isInstanceOfSatisfying(MoyeoException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIALS)
                );
    }

    @Test
    void registerLocalAllowsDuplicatedNickname() {
        AuthenticatedMember first = memberAuthService.registerLocal("moyeo1", "password123!", "현우");
        AuthenticatedMember second = memberAuthService.registerLocal("moyeo2", "password123!", "현우");

        assertThat(second.userId()).isNotEqualTo(first.userId());
        assertThat(second.nickname()).isEqualTo("현우");
    }

    @Test
    void loginLocalRejectsUnknownLoginId() {
        assertThatThrownBy(() -> memberAuthService.loginLocal("unknown", "password123!"))
                .isInstanceOfSatisfying(MoyeoException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIALS)
                );
    }

    @Test
    void loginSocialCreatesUserOnFirstLogin() {
        AuthenticatedMember member = memberAuthService.loginSocial(
                AuthProvider.KAKAO,
                "kakao-123",
                "user@example.com",
                "카카오"
        );

        assertThat(member.userId()).isNotNull();
        assertThat(member.nickname()).isEqualTo("카카오");
        assertThat(member.registered()).isTrue();
        assertThat(socialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, "kakao-123"))
                .isPresent();
    }

    @Test
    void loginSocialReturnsExistingUser() {
        AuthenticatedMember registered = memberAuthService.loginSocial(
                AuthProvider.APPLE,
                "apple-123",
                "user@example.com",
                "애플"
        );

        AuthenticatedMember loggedIn = memberAuthService.loginSocial(
                AuthProvider.APPLE,
                "apple-123",
                "changed@example.com",
                "변경"
        );

        assertThat(loggedIn.userId()).isEqualTo(registered.userId());
        assertThat(loggedIn.nickname()).isEqualTo("애플");
        assertThat(loggedIn.registered()).isFalse();
    }

    @Test
    void loginSocialRejectsSoftDeletedUser() {
        AuthenticatedMember registered = memberAuthService.loginSocial(
                AuthProvider.KAKAO,
                "deleted-kakao-123",
                "user@example.com",
                "deleted"
        );
        softDeleteUser(registered.userId());

        assertThatThrownBy(() -> memberAuthService.loginSocial(
                AuthProvider.KAKAO,
                "deleted-kakao-123",
                "changed@example.com",
                "changed"
        ))
                .isInstanceOfSatisfying(MoyeoException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIALS)
                );
    }

    private void softDeleteUser(Long userId) {
        jdbcTemplate.update("update users set deleted_at = current_timestamp where id = ?", userId);
        entityManager.clear();
    }
}
