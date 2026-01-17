package com.cotalk.domain.port.inbound.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.Message.MessageType;

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
     * 파일 메시지 전송 명령.
     *
     * @param fileUrl 파일 URL
     * @param fileName 파일명
     * @param fileSize 파일 크기
     * @param contentType 파일 MIME 타입
     * @param thumbnailUrl 썸네일 URL (이미지인 경우, 선택)
     */
    record FileMessageCommand(
            String fileUrl,
            String fileName,
            Long fileSize,
            String contentType,
            String thumbnailUrl
    ) {
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
    }
}
