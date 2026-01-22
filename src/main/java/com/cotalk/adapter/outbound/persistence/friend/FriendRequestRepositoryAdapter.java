package com.cotalk.adapter.outbound.persistence.friend;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 친구 요청 영속성 어댑터.
 * JPA를 통해 친구 요청 데이터를 저장하고 조회한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class FriendRequestRepositoryAdapter implements FriendRequestRepository {

    private final FriendRequestJpaRepository friendRequestJpaRepository;

    /**
     * 친구 요청을 저장한다.
     *
     * @param friendRequest 저장할 친구 요청 엔티티
     * @return 저장된 친구 요청 엔티티
     */
    @Override
    public FriendRequest save(FriendRequest friendRequest) {
        return friendRequestJpaRepository.save(friendRequest);
    }

    /**
     * ID로 친구 요청을 조회한다.
     *
     * @param id 친구 요청 ID
     * @return 친구 요청 (Optional)
     */
    @Override
    public Optional<FriendRequest> findById(Long id) {
        return friendRequestJpaRepository.findById(id);
    }

    /**
     * 수신자 ID로 대기 중인 친구 요청 목록을 조회한다.
     *
     * @param receiverId 수신자 ID
     * @return 대기 중인 친구 요청 목록
     */
    @Override
    public List<FriendRequest> findPendingByReceiverId(Long receiverId) {
        return friendRequestJpaRepository.findByReceiverIdAndStatus(receiverId, FriendRequest.RequestStatus.PENDING);
    }

    /**
     * 요청자 ID로 대기 중인 친구 요청 목록을 조회한다.
     *
     * @param requesterId 요청자 ID
     * @return 대기 중인 친구 요청 목록
     */
    @Override
    public List<FriendRequest> findPendingByRequesterId(Long requesterId) {
        return friendRequestJpaRepository.findByRequesterIdAndStatus(requesterId, FriendRequest.RequestStatus.PENDING);
    }

    /**
     * 요청자 ID와 수신자 ID로 친구 요청이 존재하는지 확인한다.
     *
     * @param requesterId 요청자 ID
     * @param receiverId 수신자 ID
     * @return 친구 요청 존재 여부
     */
    @Override
    public boolean existsByRequesterIdAndReceiverId(Long requesterId, Long receiverId) {
        return friendRequestJpaRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId);
    }

    /**
     * 요청자 ID, 수신자 ID, 상태로 친구 요청이 존재하는지 확인한다.
     *
     * @param requesterId 요청자 ID
     * @param receiverId  수신자 ID
     * @param status      요청 상태
     * @return 친구 요청 존재 여부
     */
    @Override
    public boolean existsByRequesterIdAndReceiverIdAndStatus(Long requesterId, Long receiverId,
                                                              FriendRequest.RequestStatus status) {
        return friendRequestJpaRepository.existsByRequesterIdAndReceiverIdAndStatus(
                requesterId, receiverId, status);
    }

    /**
     * 친구 요청을 삭제한다.
     *
     * @param friendRequest 삭제할 친구 요청 엔티티
     */
    @Override
    public void delete(FriendRequest friendRequest) {
        friendRequestJpaRepository.delete(friendRequest);
    }

    /**
     * 사용자 ID로 모든 친구 요청을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    @Override
    public void deleteByUserId(Long userId) {
        friendRequestJpaRepository.deleteByRequesterIdOrReceiverId(userId, userId);
    }
}
