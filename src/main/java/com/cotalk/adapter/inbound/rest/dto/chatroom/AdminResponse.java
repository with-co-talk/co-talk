package com.cotalk.adapter.inbound.rest.dto.chatroom;

import com.cotalk.domain.entity.ChatRoomMember;

/**
 * 관리자 관련 응답 DTO.
 *
 * @param userId  사용자 ID
 * @param role    역할
 * @param message 결과 메시지
 * @author seunggu.lee
 */
public record AdminResponse(Long userId, String role, String message) {

    /**
     * ChatRoomMember 엔티티로부터 관리자 응답을 생성합니다.
     *
     * @param member  ChatRoomMember 엔티티
     * @param message 결과 메시지
     * @return AdminResponse 인스턴스
     */
    public static AdminResponse from(ChatRoomMember member, String message) {
        return new AdminResponse(member.getUserId(), member.getRole().name(), message);
    }

    /**
     * 관리자 응답을 생성합니다.
     *
     * @param userId  사용자 ID
     * @param role    역할
     * @param message 결과 메시지
     * @return AdminResponse 인스턴스
     */
    public static AdminResponse of(Long userId, String role, String message) {
        return new AdminResponse(userId, role, message);
    }
}
