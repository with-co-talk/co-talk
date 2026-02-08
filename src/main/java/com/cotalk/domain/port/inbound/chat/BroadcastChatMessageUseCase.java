package com.cotalk.domain.port.inbound.chat;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;

import java.util.List;

/**
 * 채팅 메시지 브로드캐스트 유스케이스.
 * 저장된 메시지를 Redis Pub/Sub을 통해 채팅방 참여자들에게 브로드캐스트한다.
 *
 * <p>WebSocket 컨트롤러가 아웃바운드 포트({@code ChatMessageBroker})에
 * 직접 의존하지 않도록 인바운드 포트로 캡슐화한다.</p>
 *
 * @author seunggu.lee
 */
public interface BroadcastChatMessageUseCase {

    /**
     * 메시지를 채팅방 참여자들에게 브로드캐스트한다.
     *
     * <p>카톡/라인 방식: 발신자를 제외한 모든 멤버가 읽지 않은 상태로 시작하므로
     * unreadCount = 멤버 수 - 1 로 설정한다.</p>
     *
     * @param message         브로드캐스트할 메시지
     * @param senderNickname  발신자 닉네임 (사전 조회됨)
     * @param senderAvatarUrl 발신자 프로필 이미지 URL (사전 조회됨)
     * @param members         채팅방 멤버 목록 (사전 조회됨)
     */
    void broadcastMessage(Message message, String senderNickname, String senderAvatarUrl,
                          List<ChatRoomMember> members);
}
