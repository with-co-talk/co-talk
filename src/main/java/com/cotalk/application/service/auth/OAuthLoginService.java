package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.auth.OAuthLoginUseCase;
import com.cotalk.domain.port.outbound.AuthTokenPort;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth 로그인 서비스.
 * 소셜 로그인(카카오, 구글, 애플)을 처리하고 신규 사용자인 경우 자동 회원가입을 진행한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OAuthLoginService implements OAuthLoginUseCase {

    private final UserRepository userRepository;
    private final AuthTokenPort authTokenPort;
    private final IdGenerator idGenerator;

    /**
     * OAuth 로그인을 처리한다.
     * 기존 사용자인 경우 바로 로그인하고, 신규 사용자인 경우 자동 회원가입 후 로그인한다.
     *
     * @param provider OAuth 제공자 (KAKAO, GOOGLE, APPLE)
     * @param oauthId OAuth 제공자가 제공한 사용자 고유 ID
     * @param email 사용자 이메일
     * @param nickname 사용자 닉네임
     * @param avatarUrl 프로필 이미지 URL (선택)
     * @return 로그인 결과 (JWT 토큰, 신규 사용자 여부, 사용자 ID)
     */
    public OAuthLoginResult loginWithOAuth(
            User.OAuthProvider provider,
            String oauthId,
            String email,
            String nickname,
            String avatarUrl) {

        log.debug("OAuth login attempt: provider={}, email={}", provider, maskEmail(email));

        return userRepository.findByOAuthProviderAndOAuthId(provider, oauthId)
                .map(existingUser -> loginExistingUser(existingUser, provider))
                .orElseGet(() -> signUpAndLogin(provider, oauthId, email, nickname, avatarUrl));
    }

    private OAuthLoginResult loginExistingUser(User user, User.OAuthProvider provider) {
        if (!user.isActive()) {
            log.warn("OAuth login failed: inactive account for userId={}, provider={}", user.getId(), provider);
            throw new com.cotalk.domain.exception.InvalidCredentialsException("계정이 비활성화 또는 정지되었습니다.");
        }
        String token = authTokenPort.generateAccessToken(user.getId());
        log.info("OAuth login successful: userId={}, provider={}", user.getId(), provider);
        return new OAuthLoginResult(token, false, user.getId());
    }

    private OAuthLoginResult signUpAndLogin(
            User.OAuthProvider provider,
            String oauthId,
            String email,
            String nickname,
            String avatarUrl) {

        User newUser = User.builder()
                .id(idGenerator.nextId())
                .email(email)
                .nickname(nickname)
                .avatarUrl(avatarUrl)
                .oauthProvider(provider)
                .oauthId(oauthId)
                .build();

        User savedUser = userRepository.save(newUser);
        String token = authTokenPort.generateAccessToken(savedUser.getId());

        log.info("OAuth sign-up and login successful: userId={}, provider={}, email={}",
                savedUser.getId(), provider, maskEmail(email));
        return new OAuthLoginResult(token, true, savedUser.getId());
    }

    /**
     * 이메일 마스킹 처리 (로그 보안)
     *
     * @param email 원본 이메일
     * @return 마스킹된 이메일 (예: te**@example.com)
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        if (localPart.length() <= 2) {
            return "**@" + parts[1];
        }
        return localPart.substring(0, 2) + "**@" + parts[1];
    }

}
