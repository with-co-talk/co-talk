package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 친구 요청 JPA Repository.
 * 
 * <p>Spring Data JPA를 사용한 친구 요청 데이터 저장소 구현입니다.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Repository
public interface FriendRequestJpaRepository extends JpaRepository<FriendRequest, UUID> {
    
    /**
     * 요청자와 수신자 간의 친구 요청을 조회합니다.
     *
     * @param requesterId 요청자 ID
     * @param receiverId 수신자 ID
     * @return 친구 요청 엔티티 (없으면 empty)
     */
    Optional<FriendRequest> findByRequesterIdAndReceiverId(UUID requesterId, UUID receiverId);

    /**
     * 수신자가 받은 모든 친구 요청을 조회합니다.
     *
     * @param receiverId 수신자 ID
     * @return 친구 요청 목록
     */
    List<FriendRequest> findByReceiverId(UUID receiverId);

    /**
     * 수신자가 받은 대기 중인 친구 요청만 조회합니다.
     *
     * @param receiverId 수신자 ID
     * @return 대기 중인 친구 요청 목록
     */
    @Query("SELECT fr FROM FriendRequest fr WHERE fr.receiverId = :receiverId AND fr.status = 'PENDING' ORDER BY fr.createdAt DESC")
    List<FriendRequest> findPendingByReceiverId(@Param("receiverId") UUID receiverId);

    /**
     * 요청자와 수신자 간의 친구 요청이 존재하는지 확인합니다.
     *
     * @param requesterId 요청자 ID
     * @param receiverId 수신자 ID
     * @return 존재하면 true
     */
    boolean existsByRequesterIdAndReceiverId(UUID requesterId, UUID receiverId);
}
