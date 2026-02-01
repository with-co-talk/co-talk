package com.cotalk.adapter.inbound.rest.dto.friend;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 숨긴 친구 정보 DTO.
 * 숨긴 친구의 정보를 클라이언트에 전달하기 위한 데이터 전송 객체이다.
 *
 * @author seunggu.lee
 */
@Getter
@Builder
public class HiddenFriendDto {

    /**
     * 숨긴 친구 관계 ID
     */
    private Long id;

    /**
     * 숨긴 친구의 사용자 ID
     */
    private Long friendId;

    /**
     * 숨긴 친구의 닉네임
     */
    private String nickname;

    /**
     * 숨긴 친구의 프로필 이미지 URL
     */
    private String profileImageUrl;

    /**
     * 친구를 숨긴 일시
     */
    private LocalDateTime hiddenAt;
}
