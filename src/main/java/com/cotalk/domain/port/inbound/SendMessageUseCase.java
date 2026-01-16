package com.cotalk.domain.port.inbound;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.Message.MessageType;

public interface SendMessageUseCase {
    
    /**
     * 텍스트 메시지 전송
     */
    Message sendMessage(Long chatRoomId, Long senderId, String content);
    
    /**
     * 파일/이미지 메시지 전송
     */
    Message sendFileMessage(Long chatRoomId, Long senderId, FileMessageCommand command);
    
    /**
     * 파일 메시지 전송 명령
     */
    record FileMessageCommand(
            String fileUrl,
            String fileName,
            Long fileSize,
            String contentType,
            String thumbnailUrl  // 이미지인 경우 썸네일 URL (선택)
    ) {
        public MessageType getMessageType() {
            if (contentType != null && contentType.startsWith("image/")) {
                return MessageType.IMAGE;
            }
            return MessageType.FILE;
        }
    }
}
