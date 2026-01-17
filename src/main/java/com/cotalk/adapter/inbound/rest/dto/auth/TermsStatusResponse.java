package com.cotalk.adapter.inbound.rest.dto.auth;

import java.util.List;

/**
 * 약관 동의 상태 조회 응답 DTO.
 *
 * @param agreements 약관별 동의 상태 목록
 * @author seunggu.lee
 */
public record TermsStatusResponse(List<TermsStatusItem> agreements) {

    /**
     * TermsStatusResponse를 생성한다.
     *
     * @param agreements 약관별 동의 상태 목록
     * @return TermsStatusResponse 인스턴스
     */
    public static TermsStatusResponse of(List<TermsStatusItem> agreements) {
        return new TermsStatusResponse(agreements);
    }
}
