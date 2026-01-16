package com.cotalk.application.service;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidCredentialsException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.DeleteAccountUseCase;
import com.cotalk.domain.port.outbound.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteAccountService implements DeleteAccountUseCase {

    private final UserRepository userRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final MessageRepository messageRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void deleteAccount(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        deleteUserData(userId, user);
        log.info("Account deleted: userId={}, email={}", userId, user.getEmail());
    }

    @Override
    public void deleteAccountByAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        deleteUserData(userId, user);
        log.info("Account deleted by admin: userId={}, email={}", userId, user.getEmail());
    }

    private void deleteUserData(Long userId, User user) {
        // 관련 데이터 삭제 (순서 중요 - 외래키 제약)
        chatRoomMemberRepository.deleteByUserId(userId);
        friendRepository.deleteByUserId(userId);
        friendRequestRepository.deleteByUserId(userId);
        deviceTokenRepository.deleteByUserId(userId);
        passwordResetTokenRepository.deleteByUserId(userId);
        
        // 메시지는 익명화 처리 (채팅 히스토리 보존)
        // messageRepository.anonymizeByUserId(userId);
        
        // 사용자 삭제
        userRepository.delete(user);
    }
}
