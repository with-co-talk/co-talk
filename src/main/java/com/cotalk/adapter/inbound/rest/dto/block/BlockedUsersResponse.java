package com.cotalk.adapter.inbound.rest.dto.block;

import java.util.List;

/**
 * 차단 목록 응답 DTO.
 *
 * @param blockedUsers 차단한 사용자 목록
 * @author seunggu.lee
 */
public record BlockedUsersResponse(List<BlockedUserDto> blockedUsers) {

    /**
     * 차단 목록 응답을 생성한다.
     *
     * @param blockedUsers 차단된 사용자 DTO 목록
     * @return BlockedUsersResponse 인스턴스
     */
    public static BlockedUsersResponse of(List<BlockedUserDto> blockedUsers) {
        return new BlockedUsersResponse(blockedUsers);
    }
}
