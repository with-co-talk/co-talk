package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidCredentialsException;
import com.cotalk.domain.exception.OAuthVerificationException;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.model.VerifiedOAuthIdentity;
import com.cotalk.domain.port.inbound.auth.OAuthLoginUseCase;
import com.cotalk.domain.port.outbound.AuthTokenPort;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.MetricsPort;
import com.cotalk.domain.port.outbound.OAuthIdentityVerifier;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuthLoginServiceTest {

    private static final String TOKEN = "provider-token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthTokenPort authTokenPort;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private MetricsPort metricsPort;

    @Mock
    private OAuthIdentityVerifier oAuthIdentityVerifier;

    @InjectMocks
    private OAuthLoginService oAuthLoginService;

    @Test
    @DisplayName("검증된 식별 정보로 신규 사용자 자동 회원가입 후 로그인한다")
    void should_signUpAndLogin_when_verifiedNewOAuthUser() {
        // given
        String oauthId = "kakao_12345";
        User.OAuthProvider provider = User.OAuthProvider.KAKAO;
        VerifiedOAuthIdentity identity = new VerifiedOAuthIdentity(
                oauthId, "oauth@kakao.com", "카카오유저", "https://kakao.com/avatar.png");

        given(oAuthIdentityVerifier.verify(provider, TOKEN)).willReturn(identity);
        given(userRepository.findByOAuthProviderAndOAuthId(provider, oauthId))
                .willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(100L);
        given(userRepository.save(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(authTokenPort.generateAccessToken(any())).willReturn("jwt_token");

        // when
        OAuthLoginUseCase.OAuthLoginResult result = oAuthLoginService.loginWithOAuth(provider, TOKEN);

        // then
        assertThat(result.token()).isEqualTo("jwt_token");
        assertThat(result.isNewUser()).isTrue();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getOauthProvider()).isEqualTo(provider);
        assertThat(savedUser.getOauthId()).isEqualTo(oauthId);
        assertThat(savedUser.getEmail()).isEqualTo(new Email("oauth@kakao.com"));
        assertThat(savedUser.getNickname()).isEqualTo("카카오유저");
        assertThat(savedUser.getPasswordHash()).isNull();
    }

    @Test
    @DisplayName("검증된 식별 정보로 기존 사용자를 바로 로그인한다")
    void should_loginDirectly_when_verifiedExistingOAuthUser() {
        // given
        String oauthId = "kakao_12345";
        User.OAuthProvider provider = User.OAuthProvider.KAKAO;
        VerifiedOAuthIdentity identity = new VerifiedOAuthIdentity(
                oauthId, "oauth@kakao.com", "카카오유저", null);

        User existingUser = User.builder()
                .id(100L)
                .email(new Email("oauth@kakao.com"))
                .nickname("카카오유저")
                .oauthProvider(provider)
                .oauthId(oauthId)
                .build();

        given(oAuthIdentityVerifier.verify(provider, TOKEN)).willReturn(identity);
        given(userRepository.findByOAuthProviderAndOAuthId(provider, oauthId))
                .willReturn(Optional.of(existingUser));
        given(authTokenPort.generateAccessToken(any())).willReturn("jwt_token");

        // when
        OAuthLoginUseCase.OAuthLoginResult result = oAuthLoginService.loginWithOAuth(provider, TOKEN);

        // then
        assertThat(result.token()).isEqualTo("jwt_token");
        assertThat(result.isNewUser()).isFalse();
        assertThat(result.userId()).isEqualTo(100L);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("제공자 토큰 검증에 실패하면 OAuthVerificationException(401)을 던진다")
    void should_throwOAuthVerificationException_when_tokenVerificationFails() {
        // given
        User.OAuthProvider provider = User.OAuthProvider.GOOGLE;
        given(oAuthIdentityVerifier.verify(provider, TOKEN))
                .willThrow(new OAuthVerificationException("구글 토큰 검증에 실패했습니다."));

        // when & then
        assertThatThrownBy(() -> oAuthLoginService.loginWithOAuth(provider, TOKEN))
                .isInstanceOf(OAuthVerificationException.class);

        verify(userRepository, never()).findByOAuthProviderAndOAuthId(any(), any());
        verify(authTokenPort, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("비활성 계정이면 InvalidCredentialsException을 던진다")
    void should_throwInvalidCredentials_when_accountInactive() {
        // given
        String oauthId = "kakao_12345";
        User.OAuthProvider provider = User.OAuthProvider.KAKAO;
        VerifiedOAuthIdentity identity = new VerifiedOAuthIdentity(oauthId, "oauth@kakao.com", "카카오유저", null);

        User inactiveUser = User.builder()
                .id(100L)
                .email(new Email("oauth@kakao.com"))
                .nickname("카카오유저")
                .oauthProvider(provider)
                .oauthId(oauthId)
                .status(User.UserStatus.SUSPENDED)
                .build();

        given(oAuthIdentityVerifier.verify(provider, TOKEN)).willReturn(identity);
        given(userRepository.findByOAuthProviderAndOAuthId(provider, oauthId))
                .willReturn(Optional.of(inactiveUser));

        // when & then
        assertThatThrownBy(() -> oAuthLoginService.loginWithOAuth(provider, TOKEN))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(authTokenPort, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("제공자가 이메일/닉네임을 주지 않아도 합성값으로 회원가입에 성공한다(애플 케이스)")
    void should_signUp_when_emailAndNicknameAbsent() {
        // given
        String oauthId = "apple_sub_999";
        User.OAuthProvider provider = User.OAuthProvider.APPLE;
        VerifiedOAuthIdentity identity = new VerifiedOAuthIdentity(oauthId, null, null, null);

        given(oAuthIdentityVerifier.verify(provider, TOKEN)).willReturn(identity);
        given(userRepository.findByOAuthProviderAndOAuthId(provider, oauthId))
                .willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(101L);
        given(userRepository.save(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(authTokenPort.generateAccessToken(any())).willReturn("apple_jwt_token");

        // when
        OAuthLoginUseCase.OAuthLoginResult result = oAuthLoginService.loginWithOAuth(provider, TOKEN);

        // then
        assertThat(result.token()).isEqualTo("apple_jwt_token");
        assertThat(result.isNewUser()).isTrue();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getOauthId()).isEqualTo(oauthId);
        assertThat(savedUser.getNickname()).isNotBlank();
        assertThat(savedUser.getEmail()).isNotNull();
    }
}
