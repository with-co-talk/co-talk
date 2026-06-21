package com.cotalk.domain.port.outbound;

/**
 * 채팅 메시지 브로커 아웃바운드 포트.
 * 실시간 채팅 메시지 발행을 위한 인터페이스를 정의한다.
 * Redis Pub/Sub, Kafka 등 다양한 메시지 브로커 구현체로 교체 가능하다.
 *
 * @author seunggu.lee
 */
public interface ChatMessageBroker {

    /**
     * 채팅방에 메시지를 발행한다.
     *
     * @param roomId  채팅방 ID
     * @param message 브로드캐스트할 채팅 메시지
     */
    void publish(Long roomId, ChatBroadcastMessage message);

    /**
     * 채팅방에 반응 이벤트를 발행한다.
     *
     * @param roomId        채팅방 ID
     * @param reactionEvent 반응 이벤트
     */
    void publishReaction(Long roomId, ReactionBroadcastEvent reactionEvent);

    /**
     * 채팅방 이벤트를 발행한다.
     * <p>
     * 읽음(READ)과 같은 "채팅방 상태" 이벤트를 실시간으로 참여자에게 전달하기 위해 사용한다.
     *
     * @param roomId 채팅방 ID
     * @param event  발행할 이벤트 객체
     */
    void publishRoomEvent(Long roomId, Object event);

    /**
     * 브로드캐스트할 채팅 메시지.
     * 실시간으로 채팅방 참여자들에게 전송되는 메시지 정보를 담는다.
     *
     * @param messageId         메시지 ID
     * @param senderId          발신자 ID
     * @param senderNickname    발신자 닉네임
     * @param senderAvatarUrl   발신자 프로필 이미지 URL
     * @param roomId            채팅방 ID
     * @param content           메시지 내용
     * @param type              메시지 유형 (TEXT, IMAGE, FILE 등)
     * @param createdAtMillis   생성 시간 (밀리초)
     * @param fileUrl           파일 URL (파일 메시지인 경우)
     * @param fileName          파일명 (파일 메시지인 경우)
     * @param fileSize          파일 크기 (파일 메시지인 경우)
     * @param fileContentType   파일 MIME 타입 (파일 메시지인 경우)
     * @param thumbnailUrl      썸네일 URL (이미지/비디오 메시지인 경우)
     * @param unreadCount       읽지 않은 멤버 수 (발신자 제외)
     * @param eventType         이벤트 유형 (USER_LEFT, USER_JOINED 등, 시스템 메시지인 경우)
     * @param relatedUserId     관련 사용자 ID (나간 사용자, 참여한 사용자 등)
     * @param relatedUserNickname 관련 사용자 닉네임
     */
    /**
     * 브로드캐스트할 반응 이벤트.
     * 반응 추가/제거 시 채팅방 참여자들에게 전송되는 이벤트 정보를 담는다.
     *
     * @param schemaVersion 스키마 버전
     * @param eventId       이벤트 ID (중복 체크용)
     * @param reactionId    반응 ID
     * @param messageId     대상 메시지 ID
     * @param userId        반응한 사용자 ID
     * @param emoji         이모지 문자열
     * @param eventType     이벤트 타입 ("ADDED" 또는 "REMOVED")
     * @param timestamp     이벤트 발생 시간 (Unix timestamp, 밀리초)
     */
    record ReactionBroadcastEvent(
            Integer schemaVersion,
            String eventId,
            Long reactionId,
            Long messageId,
            Long userId,
            String emoji,
            String eventType,
            long timestamp
    ) {}

    /**
     * 브로드캐스트할 채팅 메시지.
     * 실시간으로 채팅방 참여자들에게 전송되는 메시지 정보를 담는다.
     *
     * @param messageId         메시지 ID
     * @param senderId          발신자 ID
     * @param senderNickname    발신자 닉네임
     * @param senderAvatarUrl   발신자 프로필 이미지 URL
     * @param roomId            채팅방 ID
     * @param content           메시지 내용
     * @param type              메시지 유형 (TEXT, IMAGE, FILE 등)
     * @param createdAtMillis   생성 시간 (밀리초)
     * @param fileUrl           파일 URL (파일 메시지인 경우)
     * @param fileName          파일명 (파일 메시지인 경우)
     * @param fileSize          파일 크기 (파일 메시지인 경우)
     * @param fileContentType   파일 MIME 타입 (파일 메시지인 경우)
     * @param thumbnailUrl      썸네일 URL (이미지/비디오 메시지인 경우)
     * @param unreadCount       읽지 않은 멤버 수 (발신자 제외)
     * @param eventType         이벤트 유형 (USER_LEFT, USER_JOINED 등, 시스템 메시지인 경우)
     * @param relatedUserId     관련 사용자 ID (나간 사용자, 참여한 사용자 등)
     * @param relatedUserNickname 관련 사용자 닉네임
     * @param clientMessageId   클라이언트 낙관적 전송(optimistic send) 상관관계 ID.
     *                          클라이언트가 STOMP 전송 본문에 담아 보내면 그대로 에코한다.
     *                          영속화하지 않는 일시적(transient) 값이며, 없으면 null이다.
     */
    record ChatBroadcastMessage(
            Long messageId,
            Long senderId,
            String senderNickname,
            String senderAvatarUrl,
            Long roomId,
            String content,
            String type,
            Long createdAtMillis,
            String fileUrl,
            String fileName,
            Long fileSize,
            String fileContentType,
            String thumbnailUrl,
            Integer unreadCount,
            String eventType,
            Long relatedUserId,
            String relatedUserNickname,
            String clientMessageId
    ) {}
}
