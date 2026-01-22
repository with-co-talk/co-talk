package com.cotalk.infrastructure.websocket;

import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.exception.InvalidEmojiException;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.exception.UnauthorizedException;
import com.cotalk.domain.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.time.LocalDateTime;

/**
 * WebSocket 메시지 처리 중 발생하는 예외를 처리하는 핸들러.
 *
 * <p>@MessageMapping 메서드에서 발생하는 예외를 캐치하여
 * 클라이언트에게 에러 응답을 전송한다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@ControllerAdvice
public class WebSocketExceptionHandler {

    /**
     * 인증되지 않은 사용자 예외를 처리한다.
     *
     * @param e 인증되지 않은 사용자 예외
     * @return 에러 응답
     */
    @MessageExceptionHandler(UnauthorizedException.class)
    @SendToUser("/queue/errors")
    public WebSocketErrorResponse handleUnauthorizedException(UnauthorizedException e) {
        log.warn("WebSocket unauthorized access: {}", e.getMessage());
        return new WebSocketErrorResponse("UNAUTHORIZED", e.getMessage(), LocalDateTime.now());
    }

    /**
     * 사용자를 찾을 수 없는 예외를 처리한다.
     *
     * @param e 사용자 미발견 예외
     * @return 에러 응답
     */
    @MessageExceptionHandler(UserNotFoundException.class)
    @SendToUser("/queue/errors")
    public WebSocketErrorResponse handleUserNotFoundException(UserNotFoundException e) {
        log.warn("WebSocket user not found: {}", e.getMessage());
        return new WebSocketErrorResponse("USER_NOT_FOUND", e.getMessage(), LocalDateTime.now());
    }

    /**
     * 채팅방을 찾을 수 없는 예외를 처리한다.
     *
     * @param e 채팅방 미발견 예외
     * @return 에러 응답
     */
    @MessageExceptionHandler(ChatRoomNotFoundException.class)
    @SendToUser("/queue/errors")
    public WebSocketErrorResponse handleChatRoomNotFoundException(ChatRoomNotFoundException e) {
        log.warn("WebSocket chat room not found: {}", e.getMessage());
        return new WebSocketErrorResponse("CHAT_ROOM_NOT_FOUND", e.getMessage(), LocalDateTime.now());
    }

    /**
     * 채팅방 접근 거부 예외를 처리한다.
     *
     * @param e 채팅방 접근 거부 예외
     * @return 에러 응답
     */
    @MessageExceptionHandler(ChatRoomAccessDeniedException.class)
    @SendToUser("/queue/errors")
    public WebSocketErrorResponse handleChatRoomAccessDeniedException(ChatRoomAccessDeniedException e) {
        log.warn("WebSocket chat room access denied: {}", e.getMessage());
        return new WebSocketErrorResponse("ACCESS_DENIED", e.getMessage(), LocalDateTime.now());
    }

    /**
     * 메시지를 찾을 수 없는 예외를 처리한다.
     *
     * @param e 메시지 미발견 예외
     * @return 에러 응답
     */
    @MessageExceptionHandler(MessageNotFoundException.class)
    @SendToUser("/queue/errors")
    public WebSocketErrorResponse handleMessageNotFoundException(MessageNotFoundException e) {
        log.warn("WebSocket message not found: {}", e.getMessage());
        return new WebSocketErrorResponse("MESSAGE_NOT_FOUND", e.getMessage(), LocalDateTime.now());
    }

    /**
     * 잘못된 이모지 예외를 처리한다.
     *
     * @param e 잘못된 이모지 예외
     * @return 에러 응답
     */
    @MessageExceptionHandler(InvalidEmojiException.class)
    @SendToUser("/queue/errors")
    public WebSocketErrorResponse handleInvalidEmojiException(InvalidEmojiException e) {
        log.warn("WebSocket invalid emoji: {}", e.getMessage());
        return new WebSocketErrorResponse("INVALID_EMOJI", e.getMessage(), LocalDateTime.now());
    }

    /**
     * 도메인 예외를 처리한다.
     *
     * @param e 도메인 예외
     * @return 에러 응답
     */
    @MessageExceptionHandler(DomainException.class)
    @SendToUser("/queue/errors")
    public WebSocketErrorResponse handleDomainException(DomainException e) {
        log.warn("WebSocket domain exception: {}", e.getMessage());
        return new WebSocketErrorResponse("BAD_REQUEST", e.getMessage(), LocalDateTime.now());
    }

    /**
     * 예상하지 못한 예외를 처리한다.
     *
     * @param e 예외
     * @return 에러 응답
     */
    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public WebSocketErrorResponse handleException(Exception e) {
        log.error("WebSocket unexpected error occurred", e);
        return new WebSocketErrorResponse("INTERNAL_ERROR", "메시지 처리 중 오류가 발생했습니다.", LocalDateTime.now());
    }

    /**
     * WebSocket 에러 응답 DTO.
     *
     * @param code      에러 코드
     * @param message   에러 메시지
     * @param timestamp 발생 시각
     */
    public record WebSocketErrorResponse(
            String code,
            String message,
            LocalDateTime timestamp
    ) {}
}
