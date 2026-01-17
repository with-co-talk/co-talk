package com.cotalk.adapter.inbound.rest.dto.chatroom;

/**
 * 공지사항 응답 DTO.
 *
 * @param announcement 공지사항 내용
 * @param message      결과 메시지
 * @author seunggu.lee
 */
public record AnnouncementResponse(String announcement, String message) {

    /**
     * 공지사항 응답을 생성합니다.
     *
     * @param announcement 공지사항 내용
     * @param message      결과 메시지
     * @return AnnouncementResponse 인스턴스
     */
    public static AnnouncementResponse of(String announcement, String message) {
        return new AnnouncementResponse(announcement, message);
    }
}
