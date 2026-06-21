package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.DuplicateEmailException;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.model.VerifiedOAuthIdentity;
import com.cotalk.domain.port.inbound.auth.OAuthLoginUseCase;
import com.cotalk.domain.port.outbound.AuthTokenPort;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.MetricsPort;
import com.cotalk.domain.port.outbound.OAuthIdentityVerifier;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth 로그인 서비스.
 * 소셜 로그인(카카오, 구글, 애플)을 처리하고 신규 사용자인 경우 자동 회원가입을 진행한다.
 *
 * <p>보안: 식별 정보(oauthId/email/nickname/avatar)는 클라이언트 입력을 신뢰하지 않고,
 * 제공자 토큰을 {@link OAuthIdentityVerifier}로 서버 검증하여 도출한 값만 사용한다.</p>
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
    private final MetricsPort metricsPort;
    private final OAuthIdentityVerifier oAuthIdentityVerifier;

    /**
     * OAuth 로그인을 처리한다.
     * 제공자 토큰을 검증하여 식별 정보를 도출한 뒤, 기존 사용자인 경우 바로 로그인하고
     * 신규 사용자인 경우 자동 회원가입 후 로그인한다.
     *
     * @param provider      OAuth 제공자 (KAKAO, GOOGLE, APPLE)
     * @param providerToken 제공자 토큰 (카카오: access token, 구글/애플: id_token)
     * @return 로그인 결과 (JWT 토큰, 신규 사용자 여부, 사용자 ID)
     */
    @Override
    public OAuthLoginResult loginWithOAuth(User.OAuthProvider provider, String providerToken) {
        VerifiedOAuthIdentity identity = oAuthIdentityVerifier.verify(provider, providerToken);

        log.debug("OAuth login attempt: provider={}, email={}", provider, maskEmail(identity.email()));

        return userRepository.findByOAuthProviderAndOAuthId(provider, identity.oauthId())
                .map(existingUser -> loginExistingUser(existingUser, provider))
                .orElseGet(() -> signUpAndLogin(provider, identity));
    }

    private OAuthLoginResult loginExistingUser(User user, User.OAuthProvider provider) {
        if (!user.isActive()) {
            log.warn("OAuth login failed: inactive account for userId={}, provider={}", user.getId(), provider);
            metricsPort.incrementLoginFailure();
            throw new com.cotalk.domain.exception.InvalidCredentialsException("계정이 비활성화 또는 정지되었습니다.");
        }
        String token = authTokenPort.generateAccessToken(user.getId());
        metricsPort.incrementLoginSuccess();
        log.info("OAuth login successful: userId={}, provider={}", user.getId(), provider);
        return new OAuthLoginResult(token, false, user.getId());
    }

    /**
     * 신규 사용자를 자동 회원가입한 뒤 로그인한다.
     *
     * <p>정책(보안): 조회는 (provider, oauthId)로 이뤄지므로 동일 계정의 재로그인은 위 경로에서
     * 처리되어 여기에 도달하지 않는다. 따라서 이 경로에서 검증된 이메일이 이미 다른 계정에
     * 존재한다면, 서로 다른 OAuth 신원이 같은 이메일을 주장하는 충돌 상황이다. 이때 두 계정을
     * 자동 연결(link)하면 이메일 탈취로 인한 계정 병합 위험이 있으므로, 자동 연결하지 않고
     * 명시적으로 거부한다(reject-not-link). DB 유니크 제약 위반은
     * {@link com.cotalk.infrastructure.exception.GlobalExceptionHandler}에서 409로 위생 처리된다.</p>
     *
     * @param provider OAuth 제공자
     * @param identity 검증된 식별 정보
     * @return 로그인 결과
     * @throws DuplicateEmailException 검증된 이메일이 이미 다른 계정에 존재하는 경우
     */
    private OAuthLoginResult signUpAndLogin(User.OAuthProvider provider, VerifiedOAuthIdentity identity) {
        Email email = resolveEmail(provider, identity);
        if (userRepository.existsByEmail(email.value())) {
            log.warn("OAuth 자동가입 거부: 이미 다른 계정에 존재하는 이메일 (provider={}, email={})",
                    provider, maskEmail(email.value()));
            throw new DuplicateEmailException();
        }

        User newUser = User.builder()
                .id(idGenerator.nextId())
                .email(email)
                .nickname(resolveNickname(identity))
                .avatarUrl(identity.avatarUrl())
                .oauthProvider(provider)
                .oauthId(identity.oauthId())
                .build();

        User savedUser = userRepository.save(newUser);
        String token = authTokenPort.generateAccessToken(savedUser.getId());

        metricsPort.incrementUserRegistration();
        metricsPort.incrementLoginSuccess();
        log.info("OAuth sign-up and login successful: userId={}, provider={}, email={}",
                savedUser.getId(), provider, maskEmail(identity.email()));
        return new OAuthLoginResult(token, true, savedUser.getId());
    }

    /**
     * 검증된 이메일로 {@link Email} 값 객체를 만든다.
     * 제공자(특히 애플)가 이메일을 내려주지 않은 경우 합성 이메일을 생성한다.
     *
     * @param provider OAuth 제공자
     * @param identity 검증된 식별 정보
     * @return 이메일 값 객체
     */
    private Email resolveEmail(User.OAuthProvider provider, VerifiedOAuthIdentity identity) {
        String email = identity.email();
        if (email == null || email.isBlank()) {
            String synthesized = provider.name().toLowerCase() + "_" + identity.oauthId() + "@oauth.cotalk.local";
            log.info("OAuth provider did not return email; using synthesized email for provider={}", provider);
            return new Email(synthesized);
        }
        return new Email(email);
    }

    /**
     * 검증된 닉네임을 결정한다. 제공자가 닉네임을 내려주지 않은 경우 기본 닉네임을 생성한다.
     *
     * @param identity 검증된 식별 정보
     * @return 닉네임
     */
    private String resolveNickname(VerifiedOAuthIdentity identity) {
        String nickname = identity.nickname();
        if (nickname == null || nickname.isBlank()) {
            return "user_" + identity.oauthId();
        }
        return nickname;
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
