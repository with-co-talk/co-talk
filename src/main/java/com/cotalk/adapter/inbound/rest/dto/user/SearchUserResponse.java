package com.cotalk.adapter.inbound.rest.dto.user;

import java.util.List;

/**
 * 사용자 검색 응답 DTO.
 *
 * @param users 검색된 사용자 목록
 * @author seunggu.lee
 */
public record SearchUserResponse(List<UserDto> users) {

    /**
     * 사용자 목록으로 응답을 생성한다.
     *
     * @param users 사용자 목록
     * @return SearchUserResponse 인스턴스
     */
    public static SearchUserResponse of(List<UserDto> users) {
        return new SearchUserResponse(List.copyOf(users));
    }
}
