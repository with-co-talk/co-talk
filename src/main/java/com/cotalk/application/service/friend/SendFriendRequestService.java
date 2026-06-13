package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidFriendRequestException;
import com.cotalk.domain.port.inbound.friend.SendFriendRequestUseCase;
import com.cotalk.domain.port.outbound.DistributedLockPort;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.NotificationCommandPort;
import com.cotalk.domain.validator.BlockValidator;
import com.cotalk.domain.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 친구 요청 전송 유스케이스 구현체.
 * 다른 사용자에게 친구 요청을 보낸다.
 *
 * <p>동시성 제어:
 * <ul>
 *   <li>분산락: 동일 사용자 쌍에 대한 동시 요청 방지</li>
 *   <li>TransactionTemplate: 분산락 내부에서 트랜잭션 실행하여 락-트랜잭션 범위 역전 방지</li>
 *   <li>DB UNIQUE: 중복 요청 최종 방어</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendFriendRequestService implements SendFriendRequestUseCase {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendRepository friendRepository;
    private final UserValidator userValidator;
    private final BlockValidator blockValidator;
    private final IdGenerator idGenerator;
    private final DistributedLockPort lockExecutor;
    private final NotificationCommandPort notificationCommandPort;
    private final TransactionTemplate transactionTemplate;

    /**
     * 친구 요청을 전송한다.
     * 자기 자신에게 요청 불가, 이미 친구인 경우 불가, 중복 요청 불가 조건을 검증한다.
     *
     * <p>동시성 처리:
     * <ul>
     *   <li>A→B와 B→A 동시 요청 시 분산락으로 순차 처리</li>
     *   <li>락 키는 두 사용자 ID를 정렬하여 생성 (양방향 동일 락)</li>
     * </ul>
     *
     * @param requesterId 요청을 보내는 사용자 ID
     * @param receiverId  요청을 받는 사용자 ID
     * @return 생성된 친구 요청 ID
     * @throws InvalidFriendRequestException 자기 자신에게 요청하거나, 이미 친구이거나, 중복 요청인 경우
     * @throws com.cotalk.domain.exception.UserNotFoundException 수신자를 찾을 수 없는 경우
     */
    @Override
    public Long sendFriendRequest(Long requesterId, Long receiverId) {
        userValidator.validateNotSelfAction(requesterId, receiverId, "친구 요청");
        userValidator.validateUserExists(receiverId);

        // 양방향 동일 락 키 생성 (A→B와 B→A가 같은 락)
        String lockKey = createFriendRequestLockKey(requesterId, receiverId);

        return lockExecutor.executeWithLock(lockKey, () ->
                transactionTemplate.execute(status -> createFriendRequest(requesterId, receiverId)));
    }

    /**
     * 친구 요청을 생성한다.
     * 분산락 내부에서 실행되어 동시성이 보장된다.
     *
     * @param requesterId 요청자 ID
     * @param receiverId  수신자 ID
     * @return 생성된 친구 요청 ID
     */
    protected Long createFriendRequest(Long requesterId, Long receiverId) {
        // 차단 관계 검증 (양방향): 한쪽이라도 차단했으면 친구 요청 거부
        blockValidator.validateNotBlocked(requesterId, receiverId);

        // 이미 친구인지 확인
        if (friendRepository.existsByUserIdAndFriendId(requesterId, receiverId)) {
            throw new InvalidFriendRequestException("이미 친구입니다.");
        }

        // 상대방이 이미 나에게 요청을 보냈는지 확인
        if (friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(
                receiverId, requesterId, FriendRequest.RequestStatus.PENDING)) {
            throw new InvalidFriendRequestException(
                    "상대방이 이미 친구 요청을 보냈습니다. 해당 요청을 수락해주세요.");
        }

        // 내가 이미 요청을 보냈는지 확인
        if (friendRequestRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId)) {
            throw new InvalidFriendRequestException("이미 친구 요청을 보냈습니다.");
        }

        FriendRequest friendRequest = FriendRequest.builder()
                .id(idGenerator.nextId())
                .requesterId(requesterId)
                .receiverId(receiverId)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();

        try {
            friendRequestRepository.save(friendRequest);
            log.info("Friend request created: {} -> {}", requesterId, receiverId);

            // 푸시 알림 전송 (비동기)
            sendFriendRequestPushNotification(requesterId, receiverId);
        } catch (DataIntegrityViolationException e) {
            // UNIQUE 제약 조건 위반 (동시성 최종 방어)
            log.warn("Duplicate friend request detected: {} -> {}", requesterId, receiverId);
            throw new InvalidFriendRequestException("이미 친구 요청을 보냈습니다.");
        }

        return friendRequest.getId();
    }

    /**
     * 친구 요청 푸시 알림을 전송한다.
     * 요청자의 닉네임을 조회하여 수신자에게 알림을 보낸다.
     *
     * @param requesterId 요청자 ID
     * @param receiverId  수신자 ID
     */
    private void sendFriendRequestPushNotification(Long requesterId, Long receiverId) {
        try {
            User requester = userValidator.validateUserExists(requesterId);
            notificationCommandPort.sendFriendRequestNotification(receiverId, requester.getNickname());
        } catch (Exception e) {
            // 푸시 알림 실패는 친구 요청 자체를 실패시키지 않음
            log.warn("Failed to send friend request push notification: {} -> {}", requesterId, receiverId, e);
        }
    }

    /**
     * 친구 요청 분산락 키를 생성한다.
     * 두 사용자 ID를 정렬하여 A→B와 B→A가 동일한 락을 사용하도록 한다.
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @return 분산락 키
     */
    private String createFriendRequestLockKey(Long userId1, Long userId2) {
        long smaller = Math.min(userId1, userId2);
        long larger = Math.max(userId1, userId2);
        return "friend-request:" + smaller + ":" + larger;
    }
}
