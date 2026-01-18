package com.cotalk.adapter.outbound.persistence.friend;

import com.cotalk.domain.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 친구 요청 JPA 리포지토리.
 * Spring Data JPA를 통해 친구 요청 데이터에 접근한다.
 *
 * @author seunggu.lee
 */
public interface FriendRequestJpaRepository extends JpaRepository<FriendRequest, Long> {

    /**
     * 수신자 ID와 상태로 친구 요청 목록을 조회한다.
     *
     * @param receiverId 수신자 ID
     * @param status 요청 상태
     * @return 친구 요청 목록
     */
    List<FriendRequest> findByReceiverIdAndStatus(Long receiverId, FriendRequest.RequestStatus status);

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
