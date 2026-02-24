package com.cotalk.application.service.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.user.UpdateProfileUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    private UpdateProfileUseCase updateProfileUseCase;

    @BeforeEach
    void setUp() {
        updateProfileUseCase = new UpdateProfileService(userRepository);
    }

    @Test
    @DisplayName("프로필 수정 성공 - 닉네임, 상태메시지, 아바타 URL 모두 변경")
    void should_updateProfile_when_validRequest() {
        // given
        Long userId = 1L;
        String newNickname = "새닉네임";
        String newStatusMessage = "새상태메시지";
        String newAvatarUrl = "https://example.com/new-avatar.png";
        User user = User.builder()
                .id(userId)
                .email(new Email("test@test.com"))
                .passwordHash("hashedPassword")
                .nickname("기존닉네임")
                .statusMessage("기존상태메시지")
                .avatarUrl("https://example.com/old-avatar.png")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        updateProfileUseCase.updateProfile(userId, newNickname, newStatusMessage, newAvatarUrl);

        // then
        assertThat(user.getNickname()).isEqualTo(newNickname);
        assertThat(user.getStatusMessage()).isEqualTo(newStatusMessage);
        assertThat(user.getAvatarUrl()).isEqualTo(newAvatarUrl);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("프로필 수정 성공 - 닉네임만 변경")
    void should_updateNicknameOnly_when_othersAreNull() {
        // given
        Long userId = 1L;
        String newNickname = "새닉네임";
        String originalStatusMessage = "기존상태메시지";
        String originalAvatarUrl = "https://example.com/original-avatar.png";
        User user = User.builder()
                .id(userId)
                .email(new Email("test@test.com"))
                .passwordHash("hashedPassword")
                .nickname("기존닉네임")
                .statusMessage(originalStatusMessage)
                .avatarUrl(originalAvatarUrl)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        updateProfileUseCase.updateProfile(userId, newNickname, null, null);

        // then
        assertThat(user.getNickname()).isEqualTo(newNickname);
        assertThat(user.getStatusMessage()).isEqualTo(originalStatusMessage);
        assertThat(user.getAvatarUrl()).isEqualTo(originalAvatarUrl);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("프로필 수정 성공 - 상태메시지만 변경")
    void should_updateStatusMessageOnly_when_othersAreNull() {
        // given
        Long userId = 1L;
        String originalNickname = "기존닉네임";
        String newStatusMessage = "새상태메시지";
        String originalAvatarUrl = "https://example.com/original-avatar.png";
        User user = User.builder()
                .id(userId)
                .email(new Email("test@test.com"))
                .passwordHash("hashedPassword")
                .nickname(originalNickname)
                .statusMessage("기존상태메시지")
                .avatarUrl(originalAvatarUrl)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        updateProfileUseCase.updateProfile(userId, null, newStatusMessage, null);

        // then
        assertThat(user.getNickname()).isEqualTo(originalNickname);
        assertThat(user.getStatusMessage()).isEqualTo(newStatusMessage);
        assertThat(user.getAvatarUrl()).isEqualTo(originalAvatarUrl);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("프로필 수정 성공 - 아바타 URL만 변경")
    void should_updateAvatarUrlOnly_when_othersAreNull() {
        // given
        Long userId = 1L;
        String originalNickname = "기존닉네임";
        String originalStatusMessage = "기존상태메시지";
        String newAvatarUrl = "https://example.com/new-avatar.png";
        User user = User.builder()
                .id(userId)
                .email(new Email("test@test.com"))
                .passwordHash("hashedPassword")
                .nickname(originalNickname)
                .statusMessage(originalStatusMessage)
                .avatarUrl("https://example.com/old-avatar.png")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        updateProfileUseCase.updateProfile(userId, null, null, newAvatarUrl);

        // then
        assertThat(user.getNickname()).isEqualTo(originalNickname);
        assertThat(user.getStatusMessage()).isEqualTo(originalStatusMessage);
        assertThat(user.getAvatarUrl()).isEqualTo(newAvatarUrl);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("사용자를 찾을 수 없으면 예외 발생")
    void should_throwException_when_userNotFound() {
        // given
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> updateProfileUseCase.updateProfile(userId, "닉네임", null, null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("닉네임이 빈 문자열이면 예외 발생")
    void should_throwException_when_nicknameIsEmpty() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email(new Email("test@test.com"))
                .passwordHash("hashedPassword")
                .nickname("기존닉네임")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> updateProfileUseCase.updateProfile(userId, "", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("닉네임은 비어있을 수 없습니다");
    }

    @Test
    @DisplayName("닉네임, 상태메시지, 아바타 URL이 모두 null이면 변경하지 않음")
    void should_notUpdate_when_allAreNull() {
        // given
        Long userId = 1L;
        String originalNickname = "기존닉네임";
        String originalStatusMessage = "기존상태메시지";
        String originalAvatarUrl = "https://example.com/original-avatar.png";
        User user = User.builder()
                .id(userId)
                .email(new Email("test@test.com"))
                .passwordHash("hashedPassword")
                .nickname(originalNickname)
                .statusMessage(originalStatusMessage)
                .avatarUrl(originalAvatarUrl)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        updateProfileUseCase.updateProfile(userId, null, null, null);

        // then - 값이 변경되지 않았지만 save는 호출됨
        assertThat(user.getNickname()).isEqualTo(originalNickname);
        assertThat(user.getStatusMessage()).isEqualTo(originalStatusMessage);
        assertThat(user.getAvatarUrl()).isEqualTo(originalAvatarUrl);
        verify(userRepository).save(user);
    }
}
