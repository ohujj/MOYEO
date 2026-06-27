package com.moyeo.service.member;

import com.moyeo.domain.member.AuthProvider;
import com.moyeo.domain.member.LoginAccount;
import com.moyeo.domain.member.SocialAccount;
import com.moyeo.domain.member.User;
import com.moyeo.global.error.CommonErrorCode;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.repository.member.LoginAccountRepository;
import com.moyeo.repository.member.SocialAccountRepository;
import com.moyeo.repository.member.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberAuthService {

    private final UserRepository userRepository;
    private final LoginAccountRepository loginAccountRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberAuthService(
            UserRepository userRepository,
            LoginAccountRepository loginAccountRepository,
            SocialAccountRepository socialAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.loginAccountRepository = loginAccountRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthenticatedMember registerLocal(String loginId, String rawPassword, String nickname) {
        if (loginAccountRepository.existsByLoginId(loginId)) {
            throw new MoyeoException(AuthenticationErrorCode.DUPLICATE_LOGIN_ID);
        }

        User user = userRepository.save(new User(nickname));
        LoginAccount loginAccount = new LoginAccount(user, loginId, passwordEncoder.encode(rawPassword));
        loginAccountRepository.save(loginAccount);
        return AuthenticatedMember.from(user, true);
    }

    public AuthenticatedMember loginLocal(String loginId, String rawPassword) {
        LoginAccount loginAccount = loginAccountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new MoyeoException(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIALS));

        if (!passwordEncoder.matches(rawPassword, loginAccount.getPasswordHash())) {
            throw new MoyeoException(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        return AuthenticatedMember.from(loginAccount.getUser(), false);
    }

    public AuthenticatedMember findAuthenticatedMember(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MoyeoException(CommonErrorCode.INVALID_REQUEST));
        return AuthenticatedMember.from(user, false);
    }

    @Transactional
    public AuthenticatedMember loginSocial(
            AuthProvider provider,
            String providerUserId,
            String email,
            String fallbackNickname
    ) {
        return socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(socialAccount -> AuthenticatedMember.from(socialAccount.getUser(), false))
                .orElseGet(() -> registerSocial(provider, providerUserId, email, fallbackNickname));
    }

    private AuthenticatedMember registerSocial(
            AuthProvider provider,
            String providerUserId,
            String email,
            String fallbackNickname
    ) {
        User user = userRepository.save(new User(fallbackNickname));
        SocialAccount socialAccount = new SocialAccount(user, provider, providerUserId, email);
        socialAccountRepository.save(socialAccount);
        return AuthenticatedMember.from(user, true);
    }
}
