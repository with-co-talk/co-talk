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
    default void broadcastMessage(Message message, String senderNickname, String senderAvatarUrl,
                                  List<ChatRoomMember> members) {
        broadcastMessage(message, senderNickname, senderAvatarUrl, members, null);
    }

    /**
     * 메시지를 채팅방 참여자들에게 브로드캐스트하며, 클라이언트 상관관계 ID를 에코한다.
     *
     * <p>{@code clientMessageId}는 클라이언트의 낙관적 전송(optimistic send) 매칭용 일시적
     * 상관관계 ID다. 브로드캐스트 메시지에 그대로 담아 에코하여, 발신 클라이언트가 자신의 임시
     * 메시지와 서버 에코를 정확히 매칭하도록 한다. 영속화하지 않으며 없으면 {@code null}이다.</p>
     *
     * @param message         브로드캐스트할 메시지
     * @param senderNickname  발신자 닉네임 (사전 조회됨)
     * @param senderAvatarUrl 발신자 프로필 이미지 URL (사전 조회됨)
     * @param members         채팅방 멤버 목록 (사전 조회됨)
     * @param clientMessageId 클라이언트 낙관적 전송 상관관계 ID (없으면 null)
     */
    void broadcastMessage(Message message, String senderNickname, String senderAvatarUrl,
                          List<ChatRoomMember> members, String clientMessageId);
}
