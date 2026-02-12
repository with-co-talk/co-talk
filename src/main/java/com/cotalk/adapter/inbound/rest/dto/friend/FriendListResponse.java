package com.cotalk.adapter.inbound.rest.dto.friend;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 친구 목록 응답 DTO.
 * 페이지네이션 메타데이터를 포함한다.
 *
 * @param friends       친구 목록
 * @param page          현재 페이지 번호 (0-based)
 * @param size          페이지 크기
 * @param totalElements 전체 요소 수
 * @param totalPages    전체 페이지 수
 * @author seunggu.lee
 */
public record FriendListResponse(
        List<FriendDto> friends,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * 친구 목록 응답을 생성한다. (하위 호환용)
     *
     * @param friends 친구 DTO 목록
     * @return FriendListResponse 인스턴스
     */
    public static FriendListResponse of(List<FriendDto> friends) {
        return new FriendListResponse(friends, 0, friends.size(), friends.size(), 1);
    }

    /**
     * Page 객체와 매핑된 DTO 목록으로부터 응답을 생성한다.
     *
     * @param friends 친구 DTO 목록
     * @param pageData Page 메타데이터 소스
     * @return FriendListResponse 인스턴스
     */
    public static FriendListResponse of(List<FriendDto> friends, Page<?> pageData) {
        return new FriendListResponse(
                friends,
                pageData.getNumber(),
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages()
        );
    }
}
