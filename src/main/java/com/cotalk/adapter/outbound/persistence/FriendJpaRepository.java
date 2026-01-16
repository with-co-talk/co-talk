package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 친구 관계 JPA Repository.
 * 
 * <p>Spring Data JPA를 사용한 친구 관계 데이터 저장소 구현입니다.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Repository
public interface FriendJpaRepository extends JpaRepository<Friend, UUID> {
    
    /**
     * 두 사용자 간의 친구 관계를 조회합니다.
     *
     * @param userId 첫 번째 사용자 ID
     * @param friendId 두 번째 사용자 ID
     * @return 친구 관계 엔티티 (없으면 empty)
     */
    Optional<Friend> findByUserIdAndFriendId(UUID userId, UUID friendId);

    /**
     * 사용자의 모든 친구 관계를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 친구 관계 목록
     */
    List<Friend> findByUserId(UUID userId);

    /**
     * 사용자의 수락된 친구 관계만 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 수락된 친구 관계 목록
     */
    @Query("SELECT f FROM Friend f WHERE f.userId = :userId AND f.status = 'ACCEPTED'")
    List<Friend> findAcceptedFriendsByUserId(@Param("userId") UUID userId);

    /**
     * 두 사용자 간의 친구 관계가 존재하는지 확인합니다.
     *
     * @param userId 첫 번째 사용자 ID
     * @param friendId 두 번째 사용자 ID
     * @return 존재하면 true
     */
    boolean existsByUserIdAndFriendId(UUID userId, UUID friendId);
}
