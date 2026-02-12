package com.cotalk.domain.port.inbound.friend;

import com.cotalk.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 친구 목록 조회 유스케이스.
 * 사용자의 친구 목록을 조회한다.
 *
 * @author seunggu.lee
 */
public interface GetFriendListUseCase {

    /**
     * 사용자의 친구 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 친구 목록
     */
    List<User> getFriendList(Long userId);

    /**
     * 사용자의 친구 목록을 DB 레벨 페이지네이션으로 조회한다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이지네이션 정보
     * @return 페이지네이션된 친구 목록
     */
    Page<User> getFriendList(Long userId, Pageable pageable);
}
