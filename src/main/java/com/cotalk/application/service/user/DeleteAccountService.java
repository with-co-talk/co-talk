package com.cotalk.application.service.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.PasswordMismatchException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.user.DeleteAccountUseCase;
import com.cotalk.domain.port.outbound.*;
import com.cotalk.domain.util.LogMaskingUtil;
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
    private final MessageRepository messageRepository;
    private final MessageReactionRepository messageReactionRepository;
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
     * @throws PasswordMismatchException 비밀번호가 일치하지 않는 경우
     */
    @Override
    public void deleteAccount(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new PasswordMismatchException();
        }

        deleteUserData(userId, user);
        log.info("Account deleted: userId={}, email={}", userId, LogMaskingUtil.maskEmail(user.getEmail().value()));
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
        log.info("Account deleted by admin: userId={}, email={}", userId, LogMaskingUtil.maskEmail(user.getEmail().value()));
    }

    private void deleteUserData(Long userId, User user) {
        // 관련 데이터 삭제 (순서 중요 - 외래키 제약: 자식 → 부모 순)

        // 1) 메시지 반응 정리
        //    - 이 사용자가 남긴 반응 (message_reactions.user_id)
        //    - 이 사용자의 메시지에 타인이 남긴 반응 (messages 삭제 전 선행 정리)
        messageReactionRepository.deleteByUserId(userId);
        messageReactionRepository.deleteByMessageSenderId(userId);

        // 2) 신고 정리
        //    - 이 사용자가 한 신고 (reports.reporter_id)
        //    - 이 사용자를 대상으로 한 신고 (reports.reported_user_id)
        //    - 이 사용자의 메시지를 대상으로 한 신고 (reports.reported_message_id, messages 삭제 전 선행 정리)
        reportRepository.deleteByReporterId(userId);
        reportRepository.deleteByReportedUserId(userId);
        reportRepository.deleteByReportedMessageSenderId(userId);

        // 3) 이 사용자가 보낸 메시지 삭제 (messages.sender_id)
        //    위 반응/신고를 먼저 정리했으므로 이 시점에 안전하게 삭제 가능
        messageRepository.deleteBySenderId(userId);

        chatRoomMemberRepository.deleteByUserId(userId);
        friendRepository.deleteByUserId(userId);
        friendRequestRepository.deleteByUserId(userId);
        deviceTokenRepository.deleteByUserId(userId);
        passwordResetTokenRepository.deleteByUserId(userId);
        emailVerificationTokenRepository.deleteByUserId(userId);
        blockRepository.deleteByBlockerId(userId);
        blockRepository.deleteByBlockedId(userId);
        // 이 사용자가 숨긴 레코드(user_id)와 타인이 이 사용자를 숨긴 레코드(friend_id)를 모두 삭제
        hiddenFriendRepository.deleteByUserId(userId);
        notificationSettingRepository.deleteByUserId(userId);
        termsAgreementRepository.deleteByUserId(userId);
        refreshTokenRepository.revokeAllByUserId(userId);
        profileHistoryRepository.deleteByUserId(userId);

        // 사용자 삭제
        userRepository.delete(user);
    }
}
