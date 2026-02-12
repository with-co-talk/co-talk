package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.friend.GetFriendListUseCase;
import com.cotalk.domain.port.outbound.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 친구 목록 조회 유스케이스 구현체.
 * 사용자의 친구 목록을 조회한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetFriendListService implements GetFriendListUseCase {

    private final FriendRepository friendRepository;

    /**
     * 사용자의 친구 목록을 조회한다.
     * N+1 쿼리를 방지하기 위해 JOIN 쿼리를 사용하여 한 번에 조회한다.
     *
     * @param userId 친구 목록을 조회할 사용자 ID
     * @return 친구 사용자 목록
     */
    @Override
    public List<User> getFriendList(Long userId) {
        return friendRepository.findAcceptedFriendsWithUserData(userId);
    }

    /**
     * 사용자의 친구 목록을 DB 레벨 페이지네이션으로 조회한다.
     * N+1 쿼리를 방지하기 위해 JOIN 쿼리를 사용하여 한 번에 조회한다.
     *
     * @param userId   친구 목록을 조회할 사용자 ID
     * @param pageable 페이지네이션 정보
     * @return 페이지네이션된 친구 사용자 목록
     */
    @Override
    public Page<User> getFriendList(Long userId, Pageable pageable) {
        return friendRepository.findAcceptedFriendsWithUserData(userId, pageable);
    }
}
