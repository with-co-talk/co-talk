package com.cotalk.application.service.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidCredentialsException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.user.DeleteAccountUseCase;
import com.cotalk.domain.port.outbound.*;
import com.cotalk.infrastructure.util.LogMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴 유스케이스 구현체.
 * 사용자 계정과 관련된 모든 데이터를 삭제한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteAccountService implements DeleteAccountUseCase {

    private final UserRepository userRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final BlockRepository blockRepository;
    private final ReportRepository reportRepository;
    private final HiddenFriendRepository hiddenFriendRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final TermsAgreementRepository termsAgreementRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ProfileHistoryRepository profileHistoryRepository;
    private final PasswordEncoderPort passwordEncoder;

    /**
     * 사용자 계정을 삭제한다.
     * 비밀번호 확인 후 관련된 모든 데이터를 삭제한다.
     *
     * @param userId 삭제할 사용자 ID
     * @param password 비밀번호 확인용
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     * @throws InvalidCredentialsException 비밀번호가 일치하지 않는 경우
     */
    @Override
    public void deleteAccount(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        deleteUserData(userId, user);
        log.info("Account deleted: userId={}, email={}", userId, LogMaskingUtil.maskEmail(user.getEmail()));
    }

    /**
     * 관리자가 사용자 계정을 삭제한다.
     * 비밀번호 확인 없이 관련된 모든 데이터를 삭제한다.
     *
     * @param userId 삭제할 사용자 ID
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    public void deleteAccountByAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        deleteUserData(userId, user);
        log.info("Account deleted by admin: userId={}, email={}", userId, LogMaskingUtil.maskEmail(user.getEmail()));
    }

    private void deleteUserData(Long userId, User user) {
        // 관련 데이터 삭제 (순서 중요 - 외래키 제약)
        chatRoomMemberRepository.deleteByUserId(userId);
        friendRepository.deleteByUserId(userId);
        friendRequestRepository.deleteByUserId(userId);
        deviceTokenRepository.deleteByUserId(userId);
        passwordResetTokenRepository.deleteByUserId(userId);
        emailVerificationTokenRepository.deleteByUserId(userId);
        blockRepository.deleteByBlockerId(userId);
        blockRepository.deleteByBlockedId(userId);
        reportRepository.deleteByReporterId(userId);
        hiddenFriendRepository.deleteByUserId(userId);
        notificationSettingRepository.deleteByUserId(userId);
        termsAgreementRepository.deleteByUserId(userId);
        refreshTokenRepository.revokeAllByUserId(userId);
        profileHistoryRepository.deleteByUserId(userId);

        // 사용자 삭제
        userRepository.delete(user);
    }
}
