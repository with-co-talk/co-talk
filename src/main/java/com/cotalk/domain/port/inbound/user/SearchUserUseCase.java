package com.cotalk.domain.port.inbound.user;

import com.cotalk.domain.entity.User;

import java.util.List;

/**
 * 사용자 검색 유스케이스.
 * 닉네임으로 사용자를 검색한다.
 *
 * @author seunggu.lee
 */
public interface SearchUserUseCase {

    /**
     * 닉네임으로 사용자를 검색한다.
     *
     * @param nickname 검색할 닉네임
     * @return 검색된 사용자 목록
     */
    List<User> searchByNickname(String nickname);
}
