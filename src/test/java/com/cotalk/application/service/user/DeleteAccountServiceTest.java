package com.cotalk.application.service.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.exception.PasswordMismatchException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.outbound.BlockRepository;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import com.cotalk.domain.port.outbound.HiddenFriendRepository;
import com.cotalk.domain.port.outbound.NotificationSettingRepository;
import com.cotalk.domain.port.outbound.EmailVerificationTokenRepository;
import com.cotalk.domain.port.outbound.PasswordResetTokenRepository;
import com.cotalk.domain.port.outbound.ProfileHistoryRepository;
import com.cotalk.domain.port.outbound.RefreshTokenRepository;
import com.cotalk.domain.port.outbound.ReportRepository;
import com.cotalk.domain.port.outbound.TermsAgreementRepository;
import com.cotalk.domain.port.outbound.PasswordEncoderPort;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.security.SpringPasswordEncoderAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteAccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private HiddenFriendRepository hiddenFriendRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private TermsAgreementRepository termsAgreementRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ProfileHistoryRepository profileHistoryRepository;

    private PasswordEncoderPort passwordEncoder;

    private DeleteAccountService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new SpringPasswordEncoderAdapter(new BCryptPasswordEncoder());
        service = new DeleteAccountService(
                userRepository,
                chatRoomMemberRepository,
                friendRepository,
                friendRequestRepository,
                deviceTokenRepository,
                passwordResetTokenRepository,
                emailVerificationTokenRepository,
                blockRepository,
                reportRepository,
                hiddenFriendRepository,
                notificationSettingRepository,
                termsAgreementRepository,
                refreshTokenRepository,
                profileHistoryRepository,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("올바른 비밀번호로 회원 탈퇴 성공")
    void should_deleteAccount_when_validPassword() {
        // given
        Long userId = 1L;
        String password = "password123";
        String encodedPassword = passwordEncoder.encode(password);

        User user = User.builder()
                .id(userId)
                .email(new Email("user@example.com"))
                .nickname("테스트유저")
                .passwordHash(encodedPassword)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        service.deleteAccount(userId, password);

        // then
        verify(chatRoomMemberRepository).deleteByUserId(userId);
        verify(friendRepository).deleteByUserId(userId);
        verify(friendRequestRepository).deleteByUserId(userId);
        verify(deviceTokenRepository).deleteByUserId(userId);
        verify(passwordResetTokenRepository).deleteByUserId(userId);
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("잘못된 비밀번호로 회원 탈퇴 시 예외")
    void should_throwException_when_invalidPassword() {
        // given
        Long userId = 1L;
        String correctPassword = "password123";
        String wrongPassword = "wrongPassword";
        String encodedPassword = passwordEncoder.encode(correctPassword);

        User user = User.builder()
                .id(userId)
                .email(new Email("user@example.com"))
                .nickname("테스트유저")
                .passwordHash(encodedPassword)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> service.deleteAccount(userId, wrongPassword))
                .isInstanceOf(PasswordMismatchException.class);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 탈퇴 시 예외")
    void should_throwException_when_userNotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.deleteAccount(userId, "password"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("관리자 권한으로 회원 탈퇴 성공")
    void should_deleteAccountByAdmin_when_adminRequest() {
        // given
        Long userId = 1L;

        User user = User.builder()
                .id(userId)
                .email(new Email("user@example.com"))
                .nickname("테스트유저")
                .passwordHash("hash")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        service.deleteAccountByAdmin(userId);

        // then
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("관리자가 존재하지 않는 사용자 탈퇴 시 예외")
    void should_throwException_when_userNotFoundInAdminDelete() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.deleteAccountByAdmin(userId))
                .isInstanceOf(UserNotFoundException.class);
    }
}
