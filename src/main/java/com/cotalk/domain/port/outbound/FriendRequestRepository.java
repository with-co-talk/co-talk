package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.FriendRequest;

import java.util.List;
import java.util.Optional;

/**
 * 친구 요청 레포지토리 포트.
 * 친구 요청 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface FriendRequestRepository {

    /**
     * 친구 요청을 저장한다.
     *
     * @param friendRequest 저장할 친구 요청
     * @return 저장된 친구 요청
     */
    FriendRequest save(FriendRequest friendRequest);

    /**
     * ID로 친구 요청을 조회한다.
     *
     * @param id 친구 요청 ID
     * @return 조회된 친구 요청 (Optional)
     */
    Optional<FriendRequest> findById(Long id);

    /**
     * 특정 사용자가 받은 대기 중인 친구 요청 목록을 조회한다.
     *
     * @param receiverId 수신자 ID
     * @return 대기 중인 친구 요청 목록
     */
    List<FriendRequest> findPendingByReceiverId(Long receiverId);

    /**
     * 특정 사용자가 보낸 대기 중인 친구 요청 목록을 조회한다.
     *
     * @param requesterId 요청자 ID
     * @return 대기 중인 친구 요청 목록
     */
    List<FriendRequest> findPendingByRequesterId(Long requesterId);

    /**
     * 특정 요청자와 수신자 간의 친구 요청 존재 여부를 확인한다.
     *
     * @param requesterId 요청자 ID
     * @param receiverId  수신자 ID
     * @return 존재 여부
     */
    boolean existsByRequesterIdAndReceiverId(Long requesterId, Long receiverId);

    /**
     * 특정 요청자와 수신자 간의 특정 상태 친구 요청 존재 여부를 확인한다.
     *
     * @param requesterId 요청자 ID
     * @param receiverId  수신자 ID
     * @param status      요청 상태
     * @return 존재 여부
     */
    boolean existsByRequesterIdAndReceiverIdAndStatus(Long requesterId, Long receiverId,
                                                       FriendRequest.RequestStatus status);

    /**
     * 친구 요청을 삭제한다.
     *
     * @param friendRequest 삭제할 친구 요청
     */
    void delete(FriendRequest friendRequest);

    /**
     * 특정 사용자와 관련된 모든 친구 요청을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
