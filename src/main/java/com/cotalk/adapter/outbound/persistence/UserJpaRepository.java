package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 JPA Repository.
 * 
 * <p>Spring Data JPA를 사용한 사용자 데이터 저장소 구현입니다.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Repository
public interface UserJpaRepository extends JpaRepository<User, UUID> {
    
    /**
     * 이메일로 사용자를 조회합니다.
     *
     * @param email 사용자 이메일
     * @return 사용자 엔티티 (없으면 empty)
     */
    Optional<User> findByEmail(String email);

    /**
     * 닉네임으로 사용자를 조회합니다.
     *
     * @param nickname 사용자 닉네임
     * @return 사용자 엔티티 (없으면 empty)
     */
    Optional<User> findByNickname(String nickname);

    /**
     * 이메일이 이미 존재하는지 확인합니다.
     *
     * @param email 확인할 이메일
     * @return 존재하면 true
     */
    boolean existsByEmail(String email);

    /**
     * 닉네임이 이미 존재하는지 확인합니다.
     *
     * @param nickname 확인할 닉네임
     * @return 존재하면 true
     */
    boolean existsByNickname(String nickname);

    /**
     * 키워드로 사용자를 검색합니다.
     * 
     * <p>이메일 또는 닉네임에 부분 일치 검색을 수행하며,
     * 특정 사용자는 검색 결과에서 제외됩니다.
     *
     * @param keyword 검색 키워드
     * @param excludeUserId 검색 결과에서 제외할 사용자 ID
     * @return 검색된 사용자 목록
     */
    @Query("SELECT u FROM User u WHERE (u.email LIKE %:keyword% OR u.nickname LIKE %:keyword%) AND u.id != :excludeUserId")
    List<User> searchByKeyword(@Param("keyword") String keyword, @Param("excludeUserId") UUID excludeUserId);
}
