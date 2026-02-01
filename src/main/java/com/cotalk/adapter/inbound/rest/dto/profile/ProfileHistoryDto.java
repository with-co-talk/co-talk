package com.cotalk.adapter.inbound.rest.dto.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 프로필 이력 DTO.
 * ProfileHistory 엔티티의 정보를 클라이언트에 전달하기 위한 불변 객체이다.
 *
 * @param id        프로필 이력 ID
 * @param userId    사용자 ID
 * @param type      이력 유형
 * @param url       이미지 URL
 * @param content   내용 (상태메시지)
 * @param isPrivate 나만보기 여부
 * @param isCurrent 현재 사용 중 여부
 * @param createdAt 생성일시
 * @author seunggu.lee
 */
public record ProfileHistoryDto(
        Long id,
        Long userId,
        ProfileHistoryType type,
        String url,
        String content,
        boolean isPrivate,
        boolean isCurrent,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    /**
     * ProfileHistory 엔티티로부터 DTO를 생성한다.
     *
     * @param history ProfileHistory 엔티티
     * @return 변환된 ProfileHistoryDto
     */
    public static ProfileHistoryDto from(ProfileHistory history) {
        return new ProfileHistoryDto(
                history.getId(),
                history.getUserId(),
                history.getType(),
                history.getUrl(),
                history.getContent(),
                history.isPrivate(),
                history.isCurrent(),
                history.getCreatedAt()
        );
    }
}
