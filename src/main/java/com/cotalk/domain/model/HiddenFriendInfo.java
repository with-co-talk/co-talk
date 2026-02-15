package com.cotalk.domain.model;

import java.time.LocalDateTime;

/**
 * 숨긴 친구 정보를 담는 도메인 모델.
 * Adapter 레이어의 DTO 의존을 제거하기 위해 Domain 레이어에 정의한다.
 *
 * @param id              숨긴 친구 관계 ID
 * @param friendId        숨긴 친구의 사용자 ID
 * @param nickname        숨긴 친구의 닉네임
 * @param profileImageUrl 숨긴 친구의 프로필 이미지 URL
 * @param hiddenAt        친구를 숨긴 일시
 * @author seunggu.lee
 */
public record HiddenFriendInfo(
        Long id,
        Long friendId,
        String nickname,
        String profileImageUrl,
        LocalDateTime hiddenAt
) {
}
