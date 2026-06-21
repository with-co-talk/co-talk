package com.cotalk.adapter.outbound.persistence.friend;

import com.cotalk.adapter.outbound.persistence.entity.FriendRequestJpaEntity;
import com.cotalk.domain.entity.FriendRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 친구 요청 JPA 리포지토리.
 * persistence 계층 전용이며, 도메인 반환은 Adapter에서 매핑한다.
 *
 * @author seunggu.lee
 */
public interface FriendRequestJpaRepository extends JpaRepository<FriendRequestJpaEntity, Long> {

    /**
     * 수신자 ID와 상태로 친구 요청 목록을 조회한다.
     *
     * @param receiverId 수신자 ID
     * @param status 요청 상태
     * @return 친구 요청 목록
     */
    List<FriendRequestJpaEntity> findByReceiverIdAndStatus(Long receiverId, FriendRequest.RequestStatus status);

    /**
     * 수신자 ID와 상태로 친구 요청 목록을 페이지네이션하여 조회한다.
     *
     * @param receiverId 수신자 ID
     * @param status     요청 상태
     * @param pageable   페이지네이션 정보
     * @return 페이지네이션된 친구 요청 목록
     */
    Page<FriendRequestJpaEntity> findByReceiverIdAndStatus(Long receiverId, FriendRequest.RequestStatus status, Pageable pageable);

    /**
     * 요청자 ID와 상태로 친구 요청 목록을 조회한다.
     *
     * @param requesterId 요청자 ID
     * @param status 요청 상태
     * @return 친구 요청 목록
     */
    List<FriendRequestJpaEntity> findByRequesterIdAndStatus(Long requesterId, FriendRequest.RequestStatus status);

    /**
     * 요청자 ID와 상태로 친구 요청 목록을 페이지네이션하여 조회한다.
     *
     * @param requesterId 요청자 ID
     * @param status      요청 상태
     * @param pageable    페이지네이션 정보
     * @return 페이지네이션된 친구 요청 목록
     */
    Page<FriendRequestJpaEntity> findByRequesterIdAndStatus(Long requesterId, FriendRequest.RequestStatus status, Pageable pageable);

    /**
     * 요청자 ID와 수신자 ID로 친구 요청이 존재하는지 확인한다.
     *
     * @param requesterId 요청자 ID
     * @param receiverId 수신자 ID
     * @return 친구 요청 존재 여부
     */
    boolean existsByRequesterIdAndReceiverId(Long requesterId, Long receiverId);

    /**
     * 요청자 ID, 수신자 ID, 상태로 친구 요청이 존재하는지 확인한다.
     *
     * @param requesterId 요청자 ID
     * @param receiverId  수신자 ID
     * @param status      요청 상태
     * @return 친구 요청 존재 여부
     */
    boolean existsByRequesterIdAndReceiverIdAndStatus(Long requesterId, Long receiverId,
                                                       FriendRequest.RequestStatus status);

    /**
     * 요청자 ID 또는 수신자 ID로 친구 요청을 삭제한다.
     *
     * @param requesterId 요청자 ID
     * @param receiverId 수신자 ID
     */
    void deleteByRequesterIdOrReceiverId(Long requesterId, Long receiverId);
}
