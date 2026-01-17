package com.cotalk.adapter.inbound.rest.dto.admin;

import java.util.List;

/**
 * 관리자용 사용자 목록 응답 DTO.
 *
 * @param users 사용자 목록
 * @author seunggu.lee
 */
public record AdminUsersResponse(List<AdminUserDto> users) {

    /**
     * 사용자 목록 응답을 생성한다.
     *
     * @param users 사용자 DTO 목록
     * @return AdminUsersResponse 인스턴스
     */
    public static AdminUsersResponse of(List<AdminUserDto> users) {
        return new AdminUsersResponse(users);
    }
}
