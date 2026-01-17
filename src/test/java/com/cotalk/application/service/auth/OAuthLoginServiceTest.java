package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.cotalk.common.fixture.UserTestFixture.createUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuthLoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @InjectMocks
    private OAuthLoginService oAuthLoginService;

    @Test
    @DisplayName("새로운 소셜 로그인 사용자 - 자동 회원가입 후 로그인")
    void should_signUpAndLogin_when_newOAuthUser() {
        // given
        String oauthId = "kakao_12345";
        User.OAuthProvider provider = User.OAuthProvider.KAKAO;
        String email = "oauth@kakao.com";
        String nickname = "카카오유저";
        String avatarUrl = "https://kakao.com/avatar.png";

        given(userRepository.findByOAuthProviderAndOAuthId(provider, oauthId))
                .willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(100L);
        given(userRepository.save(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(jwtTokenProvider.generateToken(any())).willReturn("jwt_token");

        // when
        OAuthLoginService.OAuthLoginResult result = oAuthLoginService.loginWithOAuth(
                provider, oauthId, email, nickname, avatarUrl);

        // then
        assertThat(result.token()).isEqualTo("jwt_token");
        assertThat(result.isNewUser()).isTrue();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getOauthProvider()).isEqualTo(provider);
        assertThat(savedUser.getOauthId()).isEqualTo(oauthId);
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getNickname()).isEqualTo(nickname);
        assertThat(savedUser.getPasswordHash()).isNull();
    }

    @Test
    @DisplayName("기존 소셜 로그인 사용자 - 바로 로그인")
    void should_loginDirectly_when_existingOAuthUser() {
        // given
        String oauthId = "kakao_12345";
        User.OAuthProvider provider = User.OAuthProvider.KAKAO;

        User existingUser = User.builder()
                .id(100L)
                .email("oauth@kakao.com")
                .nickname("카카오유저")
                .oauthProvider(provider)
                .oauthId(oauthId)
                .build();

        given(userRepository.findByOAuthProviderAndOAuthId(provider, oauthId))
                .willReturn(Optional.of(existingUser));
        given(jwtTokenProvider.generateToken(any())).willReturn("jwt_token");

        // when
        OAuthLoginService.OAuthLoginResult result = oAuthLoginService.loginWithOAuth(
                provider, oauthId, "oauth@kakao.com", "카카오유저", null);

        // then
        assertThat(result.token()).isEqualTo("jwt_token");
        assertThat(result.isNewUser()).isFalse();
        assertThat(result.userId()).isEqualTo(100L);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("구글 로그인 성공")
    void should_loginWithGoogle_when_validInput() {
        // given
        String oauthId = "google_12345";
        User.OAuthProvider provider = User.OAuthProvider.GOOGLE;
        String email = "user@gmail.com";
        String nickname = "구글유저";

        given(userRepository.findByOAuthProviderAndOAuthId(provider, oauthId))
                .willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(101L);
        given(userRepository.save(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(jwtTokenProvider.generateToken(any())).willReturn("google_jwt_token");

        // when
        OAuthLoginService.OAuthLoginResult result = oAuthLoginService.loginWithOAuth(
                provider, oauthId, email, nickname, null);

        // then
        assertThat(result.token()).isEqualTo("google_jwt_token");
        assertThat(result.isNewUser()).isTrue();
    }

    @Test
    @DisplayName("애플 로그인 성공")
    void should_loginWithApple_when_validInput() {
        // given
        String oauthId = "apple_12345";
        User.OAuthProvider provider = User.OAuthProvider.APPLE;
        String email = "user@icloud.com";
        String nickname = "애플유저";

        given(userRepository.findByOAuthProviderAndOAuthId(provider, oauthId))
                .willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(102L);
        given(userRepository.save(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(jwtTokenProvider.generateToken(any())).willReturn("apple_jwt_token");

        // when
        OAuthLoginService.OAuthLoginResult result = oAuthLoginService.loginWithOAuth(
                provider, oauthId, email, nickname, null);

        // then
        assertThat(result.token()).isEqualTo("apple_jwt_token");
        assertThat(result.isNewUser()).isTrue();
    }
}
