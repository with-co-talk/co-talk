package com.cotalk.adapter.inbound.rest.dto.user;

/**
 * 프로필 수정 요청 DTO.
 *
 * @param nickname  변경할 닉네임
 * @param avatarUrl 변경할 아바타 URL
 * @author seunggu.lee
 */
public record UpdateProfileRequest(String nickname, String avatarUrl) {
}
