package com.cotalk.application.service;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.GetFriendListUseCase;
import com.cotalk.domain.port.outbound.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetFriendListService implements GetFriendListUseCase {

    private final FriendRepository friendRepository;

    @Override
    public List<User> getFriendList(Long userId) {
        // N+1 쿼리를 방지하기 위해 JOIN 쿼리를 사용하여 한 번에 조회
        return friendRepository.findAcceptedFriendsWithUserData(userId);
    }
}
