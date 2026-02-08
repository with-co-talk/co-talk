package com.cotalk.domain.port.inbound.chat;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;

import java.util.List;

/**
 * 채팅 목록 업데이트 발행 유스케이스.
 * 새 메시지 전송 시 채팅방 참여자들의 채팅 목록을 실시간으로 업데이트한다.
 *
 * <p>배치 쿼리를 사용하여 모든 멤버의 unreadCount를 한 번에 조회하고,
 * 각 멤버에게 개별적으로 채팅 목록 업데이트 이벤트를 발행한다.</p>
 *
 * @author seunggu.lee
 */
public interface PublishChatListUpdateUseCase {

    /**
     * 채팅 목록 업데이트 이벤트를 채팅방 참여자들에게 발행한다.
     *
     * @param message        전송된 메시지
     * @param members        채팅방 멤버 목록 (중복 쿼리 방지용)
     * @param senderNickname 발신자 닉네임 (중복 쿼리 방지용)
     */
    void publishChatListUpdate(Message message, List<ChatRoomMember> members, String senderNickname);
}
