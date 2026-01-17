package com.cotalk.domain.port.inbound.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;

/**
 * 채팅방 관리 유스케이스.
 * 채팅방 이름 변경, 공지사항 설정/삭제, 관리자 권한 관리 기능을 제공한다.
 *
 * @author seunggu.lee
 */
public interface ChatRoomManagementUseCase {

    /**
     * 채팅방 이름을 변경한다. (관리자 권한 필요)
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @param newName 새로운 채팅방 이름
     * @return 수정된 채팅방
     */
    ChatRoom updateChatRoomName(Long chatRoomId, Long userId, String newName);

    /**
     * 채팅방 공지사항을 설정한다. (관리자 권한 필요)
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @param announcement 공지사항 내용
     * @return 수정된 채팅방
     */
    ChatRoom setAnnouncement(Long chatRoomId, Long userId, String announcement);

    /**
     * 채팅방 공지사항을 삭제한다. (관리자 권한 필요)
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @return 수정된 채팅방
     */
    ChatRoom clearAnnouncement(Long chatRoomId, Long userId);

    /**
     * 멤버를 관리자로 승격한다. (관리자 권한 필요)
     *
     * @param chatRoomId 채팅방 ID
     * @param adminUserId 요청하는 관리자 ID
     * @param targetUserId 승격 대상 사용자 ID
     * @return 수정된 채팅방 멤버
     */
    ChatRoomMember promoteToAdmin(Long chatRoomId, Long adminUserId, Long targetUserId);

    /**
     * 관리자 권한을 해제한다. (관리자 권한 필요)
     *
     * @param chatRoomId 채팅방 ID
     * @param adminUserId 요청하는 관리자 ID
     * @param targetUserId 권한 해제 대상 사용자 ID
     * @return 수정된 채팅방 멤버
     */
    ChatRoomMember demoteFromAdmin(Long chatRoomId, Long adminUserId, Long targetUserId);
}
