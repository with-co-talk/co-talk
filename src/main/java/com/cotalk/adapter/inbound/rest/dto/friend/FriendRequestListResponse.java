package com.cotalk.adapter.inbound.rest.dto.friend;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 친구 요청 목록 응답 DTO.
 * 페이지네이션 메타데이터를 포함한다.
 *
 * @param requests      친구 요청 목록
 * @param page          현재 페이지 번호 (0-based)
 * @param size          페이지 크기
 * @param totalElements 전체 요소 수
 * @param totalPages    전체 페이지 수
 * @author seunggu.lee
 */
public record FriendRequestListResponse(
        List<FriendRequestDto> requests,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * 친구 요청 목록 응답을 생성한다. (하위 호환용)
     *
     * @param requests 친구 요청 DTO 목록
     * @return FriendRequestListResponse 인스턴스
     */
    public static FriendRequestListResponse of(List<FriendRequestDto> requests) {
        return new FriendRequestListResponse(requests, 0, requests.size(), requests.size(), requests.isEmpty() ? 0 : 1);
    }

    /**
     * Page 객체와 매핑된 DTO 목록으로부터 응답을 생성한다.
     *
     * @param requests 친구 요청 DTO 목록
     * @param pageData Page 메타데이터 소스
     * @return FriendRequestListResponse 인스턴스
     */
    public static FriendRequestListResponse of(List<FriendRequestDto> requests, Page<?> pageData) {
        return new FriendRequestListResponse(
                requests,
                pageData.getNumber(),
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages()
        );
    }
}
