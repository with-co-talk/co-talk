package com.cotalk.domain.port.inbound.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.Message.MessageType;

import java.util.List;

/**
 * 메시지 전송 유스케이스.
 * 텍스트 및 파일/이미지 메시지 전송을 처리한다.
 *
 * @author seunggu.lee
 */
public interface SendMessageUseCase {

    /**
     * 텍스트 메시지를 전송한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param content 메시지 내용
     * @return 전송된 메시지
     */
    Message sendMessage(Long chatRoomId, Long senderId, String content);

    /**
     * 파일/이미지 메시지를 전송한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param command 파일 메시지 명령
     * @return 전송된 메시지
     */
    Message sendFileMessage(Long chatRoomId, Long senderId, FileMessageCommand command);

    /**
     * 텍스트 메시지를 전송하고 브로드캐스트 컨텍스트를 함께 반환한다.
     * 중복 DB 쿼리를 방지하기 위해 sender와 members를 한 번만 조회하여 함께 반환한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param content 메시지 내용
     * @return 메시지와 브로드캐스트에 필요한 컨텍스트
     */
    SendResult sendMessageWithContext(Long chatRoomId, Long senderId, String content);

    /**
     * 파일 메시지를 전송하고 브로드캐스트 컨텍스트를 함께 반환한다.
     * 중복 DB 쿼리를 방지하기 위해 sender와 members를 한 번만 조회하여 함께 반환한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param command 파일 메시지 명령
     * @return 메시지와 브로드캐스트에 필요한 컨텍스트
     */
    SendResult sendFileMessageWithContext(Long chatRoomId, Long senderId, FileMessageCommand command);

    /**
     * 파일 메시지를 전송하고 WebSocket 브로드캐스트까지 내부에서 처리한다.
     * REST 컨트롤러에서 브로드캐스트 로직을 서비스 레이어로 이동하기 위해 사용한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param command 파일 메시지 명령
     * @return 전송된 메시지
     */
    Message sendFileMessageAndBroadcast(Long chatRoomId, Long senderId, FileMessageCommand command);

    /**
     * 텍스트 메시지를 전송하고 WebSocket 브로드캐스트까지 내부에서 처리한다.
     * REST 컨트롤러에서 브로드캐스트 로직을 서비스 레이어로 이동하기 위해 사용한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param content 메시지 내용
     * @return 전송된 메시지
     */
    Message sendTextMessageAndBroadcast(Long chatRoomId, Long senderId, String content);

    /**
     * 메시지 전송 결과. 메시지와 함께 브로드캐스트에 필요한 컨텍스트를 포함한다.
     * WebSocket 컨트롤러가 추가 DB 쿼리 없이 Redis Pub/Sub 메시지를 발행할 수 있도록 한다.
     *
     * @param message 전송된 메시지
     * @param senderNickname 발신자 닉네임
     * @param senderAvatarUrl 발신자 프로필 이미지 URL
     * @param members 채팅방 멤버 목록
     */
    record SendResult(
        Message message,
        String senderNickname,
        String senderAvatarUrl,
        List<ChatRoomMember> members
    ) {}

    /**
     * 파일 메시지 전송 명령.
     * <p>
     * 두 가지 방식을 모두 수용한다(하위호환).
     * <ul>
     *   <li><b>신규(권장)</b>: {@code objectId}(업로드가 발급한 불투명 저장 객체 키)를 보내면
     *       서버가 소유·존재를 검증하고 URL/contentType/size를 재구성한다. 이때 {@code thumbnailObjectId}로
     *       썸네일도 object-id로 보낼 수 있다.</li>
     *   <li><b>기존</b>: {@code objectId}가 없으면 {@code fileUrl}/{@code contentType} 등을 직접 받아
     *       서버사이드 URL 화이트리스트 검증(#166)을 수행한다.</li>
     * </ul>
     * {@code contentType}/{@code fileSize}는 object-id 방식에서도 저장소 메타가 없을 때의 폴백 힌트로 쓰인다.
     * </p>
     *
     * @param objectId          불투명 저장 객체 키(신규 방식, 선택)
     * @param thumbnailObjectId 썸네일 불투명 저장 객체 키(신규 방식, 선택)
     * @param fileUrl 파일 URL(기존 방식)
     * @param fileName 파일명(표시용)
     * @param fileSize 파일 크기
     * @param contentType 파일 MIME 타입
     * @param thumbnailUrl 썸네일 URL (이미지인 경우, 선택; 기존 방식)
     */
    record FileMessageCommand(
            String objectId,
            String thumbnailObjectId,
            String fileUrl,
            String fileName,
            Long fileSize,
            String contentType,
            String thumbnailUrl
    ) {
        /**
         * 기존 방식(object-id 없음) 명령을 생성한다(하위호환 팩토리).
         *
         * @param fileUrl 파일 URL
         * @param fileName 파일명
         * @param fileSize 파일 크기
         * @param contentType 파일 MIME 타입
         * @param thumbnailUrl 썸네일 URL
         * @return object-id가 없는 FileMessageCommand
         */
        public static FileMessageCommand ofUrl(String fileUrl, String fileName, Long fileSize,
                                               String contentType, String thumbnailUrl) {
            return new FileMessageCommand(null, null, fileUrl, fileName, fileSize, contentType, thumbnailUrl);
        }

        /**
         * 불투명 식별자(object-id) 방식 여부.
         *
         * @return {@code objectId}가 존재하면 true
         */
        public boolean usesObjectId() {
            return hasText(objectId);
        }

        /**
         * 메시지 타입을 결정한다.
         *
         * @return 이미지인 경우 IMAGE, 그 외에는 FILE
         */
        public MessageType getMessageType() {
            if (contentType != null && contentType.startsWith("image/")) {
                return MessageType.IMAGE;
            }
            return MessageType.FILE;
        }

        /**
         * 문자열이 null/공백이 아닌 실제 값을 가지는지 확인한다.
         *
         * @param value 검사할 문자열
         * @return null도 공백도 아니면 true
         */
        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
