package com.cotalk.adapter.inbound.rest.dto.admin;

import com.cotalk.domain.model.PageResult;

import java.util.List;

/**
 * 관리자용 사용자 목록 응답 DTO.
 * 페이지네이션 메타데이터를 포함한다.
 *
 * @param users         사용자 목록
 * @param page          현재 페이지 번호 (0-based)
 * @param size          페이지 크기
 * @param totalElements 전체 요소 수
 * @param totalPages    전체 페이지 수
 * @author seunggu.lee
 */
public record AdminUsersResponse(
        List<AdminUserDto> users,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * 사용자 목록 응답을 생성한다. (하위 호환용)
     *
     * @param users 사용자 DTO 목록
     * @return AdminUsersResponse 인스턴스
     */
    public static AdminUsersResponse of(List<AdminUserDto> users) {
        return new AdminUsersResponse(users, 0, users.size(), users.size(), 1);
    }

    /**
     * PageResult 객체와 매핑된 DTO 목록으로부터 응답을 생성한다.
     *
     * @param users    사용자 DTO 목록
     * @param pageData PageResult 메타데이터 소스
     * @return AdminUsersResponse 인스턴스
     */
    public static AdminUsersResponse of(List<AdminUserDto> users, PageResult<?> pageData) {
        return new AdminUsersResponse(
                users,
                pageData.page(),
                pageData.size(),
                pageData.totalElements(),
                pageData.totalPages()
        );
    }
}
