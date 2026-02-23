package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.exception.FriendNotFoundException;
import com.cotalk.domain.exception.InvalidFriendRequestException;
import com.cotalk.domain.port.inbound.friend.AcceptFriendRequestUseCase;
import com.cotalk.domain.port.outbound.DistributedLockPort;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 친구 요청 수락 유스케이스 구현체.
 * 친구 요청을 수락하고 양방향 친구 관계를 생성한다.
 *
 * <p>동시성 제어:
 * <ul>
 *   <li>분산락: 동일 친구 요청에 대한 동시 수락 방지</li>
 *   <li>TransactionTemplate: 분산락 내부에서 트랜잭션 실행하여 락-트랜잭션 범위 역전 방지</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
public class AcceptFriendRequestService implements AcceptFriendRequestUseCase {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendRepository friendRepository;
    private final IdGenerator idGenerator;
    private final DistributedLockPort lockExecutor;
    private final TransactionTemplate transactionTemplate;

    /**
     * 친구 요청을 수락한다.
     * 요청이 유효하고 본인이 받은 요청인 경우에만 수락 처리하며,
     * 양방향 친구 관계(요청자-수신자, 수신자-요청자)를 생성한다.
     *
     * <p>동시성 처리:
     * <ul>
     *   <li>요청 ID 기반 분산락으로 동시 수락 방지</li>
     *   <li>분산락 내부에서 TransactionTemplate으로 트랜잭션 실행</li>
     * </ul>
     *
     * @param receiverId 요청을 받은 사용자 ID
     * @param requestId  친구 요청 ID
     * @return 생성된 친구 관계 ID
     * @throws FriendNotFoundException       친구 요청을 찾을 수 없는 경우
     * @throws InvalidFriendRequestException 본인이 받은 요청이 아니거나 이미 처리된 요청인 경우
     */
    @Override
    public Long acceptFriendRequest(Long receiverId, Long requestId) {
        String lockKey = "friend-accept:" + requestId;

        return lockExecutor.executeWithLock(lockKey, () ->
                transactionTemplate.execute(status -> doAcceptFriendRequest(receiverId, requestId)));
    }

    /**
     * 친구 요청 수락을 실행한다.
     * 분산락 내부에서 실행되어 동시성이 보장된다.
     *
     * @param receiverId 요청을 받은 사용자 ID
     * @param requestId  친구 요청 ID
     * @return 생성된 친구 관계 ID
     */
    private Long doAcceptFriendRequest(Long receiverId, Long requestId) {
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new FriendNotFoundException("친구 요청을 찾을 수 없습니다."));

        if (!friendRequest.getReceiverId().equals(receiverId)) {
            throw new InvalidFriendRequestException("본인이 받은 친구 요청만 수락할 수 있습니다.");
        }

        if (!friendRequest.isPending()) {
            throw new InvalidFriendRequestException("이미 처리된 친구 요청입니다.");
        }

        friendRequest.accept();
        friendRequestRepository.save(friendRequest);

        Friend friend1 = Friend.builder()
                .id(idGenerator.nextId())
                .userId(friendRequest.getRequesterId())
                .friendId(friendRequest.getReceiverId())
                .status(Friend.FriendStatus.ACCEPTED)
                .build();

        Friend friend2 = Friend.builder()
                .id(idGenerator.nextId())
                .userId(friendRequest.getReceiverId())
                .friendId(friendRequest.getRequesterId())
                .status(Friend.FriendStatus.ACCEPTED)
                .build();

        friendRepository.save(friend1);
        friendRepository.save(friend2);

        return friend1.getId();
    }
}
