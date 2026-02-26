package com.cotalk.adapter.inbound.rest.dto.chatroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 공지사항 설정 요청 DTO.
 *
 * @param announcement 공지사항 내용
 * @author seunggu.lee
 */
public record SetAnnouncementRequest(
        @NotBlank(message = "공지사항 내용은 필수입니다.")
        @Size(max = 500, message = "공지사항은 500자를 초과할 수 없습니다.")
        String announcement
) {

    /**
     * 공지사항 설정 요청을 생성합니다.
     *
     * @param announcement 공지사항 내용
     * @return SetAnnouncementRequest 인스턴스
     */
    public static SetAnnouncementRequest of(String announcement) {
        return new SetAnnouncementRequest(announcement);
    }
}
