package com.cotalk.adapter.inbound.rest.dto.profile;

import java.util.List;

/**
 * 프로필 이력 목록 응답 DTO.
 *
 * @param histories 프로필 이력 목록
 * @param total     전체 개수
 * @author seunggu.lee
 */
public record ProfileHistoryListResponse(
        List<ProfileHistoryDto> histories,
        int total
) {

    /**
     * 이력 목록으로부터 응답을 생성한다.
     *
     * @param histories 프로필 이력 DTO 목록
     * @return 목록 응답
     */
    public static ProfileHistoryListResponse of(List<ProfileHistoryDto> histories) {
        return new ProfileHistoryListResponse(histories, histories.size());
    }
}
