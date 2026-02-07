package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.AuthTokenPort;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth 로그인 서비스.
 * 소셜 로그인(카카오, 구글, 애플)을 처리하고 신규 사용자인 경우 자동 회원가입을 진행한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OAuthLoginService {

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

        return userRepository.findByOAuthProviderAndOAuthId(provider, oauthId)
                .map(existingUser -> loginExistingUser(existingUser))
                .orElseGet(() -> signUpAndLogin(provider, oauthId, email, nickname, avatarUrl));
    }

    private OAuthLoginResult loginExistingUser(User user) {
        String token = authTokenPort.generateAccessToken(user.getId());
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

        return new OAuthLoginResult(token, true, savedUser.getId());
    }

    /**
     * OAuth 로그인 결과를 담는 레코드.
     *
     * @param token JWT 토큰
     * @param isNewUser 신규 사용자 여부
     * @param userId 사용자 ID
     */
    public record OAuthLoginResult(String token, boolean isNewUser, Long userId) {}
}
