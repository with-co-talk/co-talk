package com.cotalk.adapter.inbound.rest.dto.profile;

import com.cotalk.domain.entity.ProfileHistoryType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 프로필 이력 생성 요청 DTO.
 *
 * @param type       이력 유형 (필수)
 * @param url        이미지 URL (AVATAR, BACKGROUND인 경우)
 * @param content    내용 (STATUS_MESSAGE인 경우, 최대 60자)
 * @param isPrivate  나만보기 여부
 * @param setCurrent 현재 프로필로 설정 여부
 * @author seunggu.lee
 */
public record CreateProfileHistoryRequest(
        @NotNull(message = "이력 유형은 필수입니다.")
        ProfileHistoryType type,

        @Size(max = 500, message = "URL은 최대 500자입니다.")
        String url,

        @Size(max = 60, message = "상태메시지는 최대 60자입니다.")
        String content,

        boolean isPrivate,

        boolean setCurrent
) {
    /**
     * 기본값 설정을 위한 컴팩트 생성자.
     */
    public CreateProfileHistoryRequest {
        if (setCurrent == false) {
            setCurrent = true; // 기본값: 현재 프로필로 설정
        }
    }
}
