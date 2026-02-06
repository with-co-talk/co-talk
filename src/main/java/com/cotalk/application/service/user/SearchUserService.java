package com.cotalk.application.service.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.user.SearchUserUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자 검색 유스케이스 구현체.
 * 닉네임으로 사용자를 검색한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchUserService implements SearchUserUseCase {

    private static final int MAX_SEARCH_RESULTS = 50;

    private final UserRepository userRepository;

    /**
     * 닉네임으로 사용자를 검색한다.
     * DB 레벨에서 최대 {@value MAX_SEARCH_RESULTS}건으로 제한한다.
     *
     * @param nickname 검색할 닉네임 (부분 일치)
     * @return 검색된 사용자 목록 (최대 50건)
     */
    @Override
    public List<User> searchByNickname(String nickname) {
        return userRepository.findByNicknameContaining(nickname, MAX_SEARCH_RESULTS);
    }
}
