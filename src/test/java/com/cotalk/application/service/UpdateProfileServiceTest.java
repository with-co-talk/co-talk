package com.cotalk.application.service;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.UpdateProfileUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
    @DisplayName("프로필 수정 성공 - 닉네임과 아바타 URL 모두 변경")
    void should_updateProfile_when_validRequest() {
        // given
        Long userId = 1L;
        String newNickname = "새닉네임";
        String newAvatarUrl = "https://example.com/new-avatar.png";
        User user = User.builder()
                .id(userId)
                .email("test@test.com")
                .passwordHash("hashedPassword")
                .nickname("기존닉네임")
                .avatarUrl("https://example.com/old-avatar.png")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        updateProfileUseCase.updateProfile(userId, newNickname, newAvatarUrl);

        // then
        assertThat(user.getNickname()).isEqualTo(newNickname);
        assertThat(user.getAvatarUrl()).isEqualTo(newAvatarUrl);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("프로필 수정 성공 - 닉네임만 변경")
    void should_updateNicknameOnly_when_avatarUrlIsNull() {
        // given
        Long userId = 1L;
        String newNickname = "새닉네임";
        String originalAvatarUrl = "https://example.com/original-avatar.png";
        User user = User.builder()
                .id(userId)
                .email("test@test.com")
                .passwordHash("hashedPassword")
                .nickname("기존닉네임")
                .avatarUrl(originalAvatarUrl)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        updateProfileUseCase.updateProfile(userId, newNickname, null);

        // then
        assertThat(user.getNickname()).isEqualTo(newNickname);
        assertThat(user.getAvatarUrl()).isEqualTo(originalAvatarUrl);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("프로필 수정 성공 - 아바타 URL만 변경")
    void should_updateAvatarUrlOnly_when_nicknameIsNull() {
        // given
        Long userId = 1L;
        String originalNickname = "기존닉네임";
        String newAvatarUrl = "https://example.com/new-avatar.png";
        User user = User.builder()
                .id(userId)
                .email("test@test.com")
                .passwordHash("hashedPassword")
                .nickname(originalNickname)
                .avatarUrl("https://example.com/old-avatar.png")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        updateProfileUseCase.updateProfile(userId, null, newAvatarUrl);

        // then
        assertThat(user.getNickname()).isEqualTo(originalNickname);
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
        assertThatThrownBy(() -> updateProfileUseCase.updateProfile(userId, "닉네임", null))
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
                .email("test@test.com")
                .passwordHash("hashedPassword")
                .nickname("기존닉네임")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> updateProfileUseCase.updateProfile(userId, "", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("닉네임은 비어있을 수 없습니다");
    }
}
