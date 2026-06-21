package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.PageQuery;
import com.cotalk.domain.model.PageResult;

import java.util.List;
import java.util.Optional;

/**
 * 친구 관계 레포지토리 포트.
 * 친구 관계 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface FriendRepository {

    /**
     * 친구 관계를 저장한다.
     *
     * @param friend 저장할 친구 관계
     * @return 저장된 친구 관계
     */
    Friend save(Friend friend);

    /**
     * ID로 친구 관계를 조회한다.
     *
     * @param id 친구 관계 ID
     * @return 조회된 친구 관계 (Optional)
     */
    Optional<Friend> findById(Long id);

    /**
     * 사용자 ID와 친구 ID로 친구 관계를 조회한다.
     *
     * @param userId   사용자 ID
     * @param friendId 친구 ID
     * @return 친구 관계 (Optional)
     */
    Optional<Friend> findByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 사용자의 수락된 친구 관계 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 수락된 친구 관계 목록
     */
    List<Friend> findAcceptedFriendsByUserId(Long userId);

    /**
     * 사용자의 수락된 친구 목록을 사용자 정보와 함께 조회한다.
     * N+1 문제를 방지하기 위해 JOIN FETCH를 사용한다.
     *
     * @param userId 사용자 ID
     * @return 친구의 사용자 정보 목록
     */
    List<User> findAcceptedFriendsWithUserData(Long userId);

    /**
     * 사용자의 수락된 친구 목록을 DB 레벨 페이지네이션으로 조회한다.
     * N+1 문제를 방지하기 위해 JOIN을 사용한다.
     *
     * @param userId 사용자 ID
     * @param query  페이지네이션 정보
     * @return 페이지네이션된 친구 사용자 정보
     */
    PageResult<User> findAcceptedFriendsWithUserData(Long userId, PageQuery query);

    /**
     * 친구 관계 존재 여부를 확인한다.
     *
     * @param userId   사용자 ID
     * @param friendId 친구 ID
     * @return 존재 여부
     */
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 친구 관계를 삭제한다.
     *
     * @param friend 삭제할 친구 관계
     */
    void delete(Friend friend);

    /**
     * 특정 사용자의 모든 친구 관계를 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
