package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;

import java.util.List;
import java.util.Optional;

/**
 * 프로필 이력 레포지토리 포트.
 * 프로필 이력 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface ProfileHistoryRepository {

    /**
     * 프로필 이력을 저장한다.
     *
     * @param profileHistory 저장할 프로필 이력
     * @return 저장된 프로필 이력
     */
    ProfileHistory save(ProfileHistory profileHistory);

    /**
     * ID로 프로필 이력을 조회한다.
     *
     * @param id 프로필 이력 ID
     * @return 조회된 프로필 이력 (Optional)
     */
    Optional<ProfileHistory> findById(Long id);

    /**
     * 사용자 ID와 유형으로 프로필 이력 목록을 조회한다.
     * 결과는 생성일 내림차순으로 정렬된다.
     *
     * @param userId 사용자 ID
     * @param type 프로필 이력 유형
     * @return 프로필 이력 목록
     */
    List<ProfileHistory> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, ProfileHistoryType type);

    /**
     * 사용자 ID로 모든 프로필 이력을 조회한다.
     * 결과는 생성일 내림차순으로 정렬된다.
     *
     * @param userId 사용자 ID
     * @return 프로필 이력 목록
     */
    List<ProfileHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자 ID와 유형으로 현재 사용 중인 프로필 이력을 조회한다.
     *
     * @param userId 사용자 ID
     * @param type 프로필 이력 유형
     * @return 현재 프로필 이력 (Optional)
     */
    Optional<ProfileHistory> findByUserIdAndTypeAndIsCurrentTrue(Long userId, ProfileHistoryType type);

    /**
     * 프로필 이력을 삭제한다.
     *
     * @param profileHistory 삭제할 프로필 이력
     */
    void delete(ProfileHistory profileHistory);

    /**
     * 사용자 ID와 유형으로 프로필 이력 수를 조회한다.
     *
     * @param userId 사용자 ID
     * @param type 프로필 이력 유형
     * @return 프로필 이력 수
     */
    long countByUserIdAndType(Long userId, ProfileHistoryType type);
}
