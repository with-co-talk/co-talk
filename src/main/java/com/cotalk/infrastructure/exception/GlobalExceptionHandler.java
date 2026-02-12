package com.cotalk.infrastructure.exception;

import com.cotalk.domain.exception.BlockNotFoundException;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.exception.EmailNotVerifiedException;
import com.cotalk.domain.exception.DuplicateEmailException;
import com.cotalk.domain.exception.DuplicateNicknameException;
import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.domain.exception.FriendNotFoundException;
import com.cotalk.domain.exception.FriendRequestAccessDeniedException;
import com.cotalk.domain.exception.FriendRequestNotFoundException;
import com.cotalk.domain.exception.InvalidBlockException;
import com.cotalk.domain.exception.InvalidCredentialsException;
import com.cotalk.domain.exception.InvalidEmojiException;
import com.cotalk.domain.exception.PasswordMismatchException;
import com.cotalk.domain.exception.InvalidFriendRequestException;
import com.cotalk.domain.exception.InvalidEmailVerificationTokenException;
import com.cotalk.domain.exception.InvalidPasswordResetTokenException;
import com.cotalk.domain.exception.InvalidRefreshTokenException;
import com.cotalk.domain.exception.InvalidReportException;
import com.cotalk.domain.exception.MessageAccessDeniedException;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.exception.MessageReactionNotFoundException;
import com.cotalk.domain.exception.RateLimitExceededException;
import com.cotalk.domain.exception.ReportNotFoundException;
import com.cotalk.domain.exception.ResourceAccessDeniedException;
import com.cotalk.domain.exception.TermsAgreementException;
import com.cotalk.domain.exception.UnauthorizedException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.infrastructure.lock.DistributedLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * 전역 예외 처리 핸들러.
 * 모든 컨트롤러에서 발생하는 예외를 일관된 형식으로 처리한다.
 *
 * <p>도메인 예외, 유효성 검사 예외, 시스템 예외 등을 적절한 HTTP 상태 코드와
 * 에러 응답으로 변환하여 클라이언트에 반환한다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 이메일 중복 예외를 처리한다.
     *
     * @param e 이메일 중복 예외
     * @return 409 Conflict 응답
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmailException(DuplicateEmailException e) {
        log.warn("Duplicate email: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getMessage(), "DUPLICATE_EMAIL", LocalDateTime.now()));
    }

    /**
     * 비밀번호 불일치 예외를 처리한다.
     * 로그인이 아닌 비밀번호 확인 시 사용한다 (회원탈퇴, 비밀번호 변경 등).
     *
     * @param e 비밀번호 불일치 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatchException(PasswordMismatchException e) {
        log.warn("Password mismatch: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), "PASSWORD_MISMATCH", LocalDateTime.now()));
    }

    /**
     * 잘못된 인증 정보 예외를 처리한다.
     * 보안상 이메일 존재 여부를 노출하지 않기 위해 일반적인 메시지를 반환한다.
     *
     * @param e 잘못된 인증 정보 예외
     * @return 401 Unauthorized 응답
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(InvalidCredentialsException e) {
        log.warn("Authentication failed");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("이메일 또는 비밀번호가 올바르지 않습니다.", "INVALID_CREDENTIALS", LocalDateTime.now()));
    }

    /**
     * 인증되지 않은 사용자 예외를 처리한다.
     *
     * @param e 인증되지 않은 사용자 예외
     * @return 401 Unauthorized 응답
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("Unauthorized access: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(e.getMessage(), "UNAUTHORIZED", LocalDateTime.now()));
    }

    /**
     * 리소스 접근 거부 예외를 처리한다.
     * 인증된 사용자가 자신의 리소스가 아닌 다른 사용자의 리소스에 접근할 때 발생한다.
     *
     * @param e 리소스 접근 거부 예외
     * @return 403 Forbidden 응답
     */
    @ExceptionHandler(ResourceAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccessDeniedException(ResourceAccessDeniedException e) {
        log.warn("Resource access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(e.getMessage(), "ACCESS_DENIED", LocalDateTime.now()));
    }

    /**
     * 유효하지 않은 Refresh Token 예외를 처리한다.
     *
     * @param e 유효하지 않은 Refresh Token 예외
     * @return 401 Unauthorized 응답
     */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshTokenException(InvalidRefreshTokenException e) {
        log.warn("Invalid refresh token: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(e.getMessage(), "INVALID_REFRESH_TOKEN", LocalDateTime.now()));
    }

    /**
     * 사용자를 찾을 수 없는 예외를 처리한다.
     *
     * @param e 사용자 미발견 예외
     * @return 404 Not Found 응답
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e) {
        log.warn("User not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), "USER_NOT_FOUND", LocalDateTime.now()));
    }

    /**
     * 채팅방을 찾을 수 없는 예외를 처리한다.
     *
     * @param e 채팅방 미발견 예외
     * @return 404 Not Found 응답
     */
    @ExceptionHandler(ChatRoomNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChatRoomNotFoundException(ChatRoomNotFoundException e) {
        log.warn("ChatRoom not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), "CHAT_ROOM_NOT_FOUND", LocalDateTime.now()));
    }

    /**
     * 채팅방 접근 거부 예외를 처리한다.
     *
     * @param e 채팅방 접근 거부 예외
     * @return 403 Forbidden 응답
     */
    @ExceptionHandler(ChatRoomAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleChatRoomAccessDeniedException(ChatRoomAccessDeniedException e) {
        log.warn("ChatRoom access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(e.getMessage(), "ACCESS_DENIED", LocalDateTime.now()));
    }

    /**
     * 친구를 찾을 수 없는 예외를 처리한다.
     *
     * @param e 친구 미발견 예외
     * @return 404 Not Found 응답
     */
    @ExceptionHandler(FriendNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFriendNotFoundException(FriendNotFoundException e) {
        log.warn("Friend not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), "FRIEND_NOT_FOUND", LocalDateTime.now()));
    }

    /**
     * 친구 요청을 찾을 수 없는 예외를 처리한다.
     *
     * @param e 친구 요청 미발견 예외
     * @return 404 Not Found 응답
     */
    @ExceptionHandler(FriendRequestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFriendRequestNotFoundException(FriendRequestNotFoundException e) {
        log.warn("Friend request not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), "FRIEND_REQUEST_NOT_FOUND", LocalDateTime.now()));
    }

    /**
     * 친구 요청 접근 거부 예외를 처리한다.
     *
     * @param e 친구 요청 접근 거부 예외
     * @return 403 Forbidden 응답
     */
    @ExceptionHandler(FriendRequestAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleFriendRequestAccessDeniedException(FriendRequestAccessDeniedException e) {
        log.warn("Friend request access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(e.getMessage(), "FRIEND_REQUEST_ACCESS_DENIED", LocalDateTime.now()));
    }

    /**
     * 잘못된 친구 요청 예외를 처리한다.
     *
     * @param e 잘못된 친구 요청 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(InvalidFriendRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFriendRequestException(InvalidFriendRequestException e) {
        log.warn("Invalid friend request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), "INVALID_FRIEND_REQUEST", LocalDateTime.now()));
    }

    /**
     * 닉네임 중복 예외를 처리한다.
     *
     * @param e 닉네임 중복 예외
     * @return 409 Conflict 응답
     */
    @ExceptionHandler(DuplicateNicknameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateNicknameException(DuplicateNicknameException e) {
        log.warn("Duplicate nickname: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getMessage(), "DUPLICATE_NICKNAME", LocalDateTime.now()));
    }

    /**
     * 약관 동의 예외를 처리한다.
     *
     * @param e 약관 동의 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(TermsAgreementException.class)
    public ResponseEntity<ErrorResponse> handleTermsAgreementException(TermsAgreementException e) {
        log.warn("Terms agreement required: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), "TERMS_AGREEMENT_REQUIRED", LocalDateTime.now()));
    }

    /**
     * 파일 업로드 예외를 처리한다.
     *
     * @param e 파일 업로드 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUploadException(FileUploadException e) {
        log.warn("File upload failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), "FILE_UPLOAD_ERROR", LocalDateTime.now()));
    }

    /**
     * 이메일 미인증 예외를 처리한다.
     *
     * @param e 이메일 미인증 예외
     * @return 403 Forbidden 응답
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerifiedException(EmailNotVerifiedException e) {
        log.warn("Email not verified: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(e.getMessage(), "EMAIL_NOT_VERIFIED", LocalDateTime.now()));
    }

    /**
     * 유효하지 않은 이메일 인증 토큰 예외를 처리한다.
     *
     * @param e 유효하지 않은 이메일 인증 토큰 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(InvalidEmailVerificationTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEmailVerificationTokenException(InvalidEmailVerificationTokenException e) {
        log.warn("Invalid email verification token: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), "INVALID_EMAIL_VERIFICATION_TOKEN", LocalDateTime.now()));
    }

    /**
     * 잘못된 비밀번호 재설정 토큰 예외를 처리한다.
     *
     * @param e 잘못된 비밀번호 재설정 토큰 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordResetTokenException(InvalidPasswordResetTokenException e) {
        log.warn("Invalid password reset token: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), "INVALID_PASSWORD_RESET_TOKEN", LocalDateTime.now()));
    }

    /**
     * 메시지를 찾을 수 없는 예외를 처리한다.
     *
     * @param e 메시지 미발견 예외
     * @return 404 Not Found 응답
     */
    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotFoundException(MessageNotFoundException e) {
        log.warn("Message not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), "MESSAGE_NOT_FOUND", LocalDateTime.now()));
    }

    /**
     * 메시지 접근 거부 예외를 처리한다.
     *
     * @param e 메시지 접근 거부 예외
     * @return 403 Forbidden 응답
     */
    @ExceptionHandler(MessageAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleMessageAccessDeniedException(MessageAccessDeniedException e) {
        log.warn("Message access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(e.getMessage(), "MESSAGE_ACCESS_DENIED", LocalDateTime.now()));
    }

    /**
     * 잘못된 이모지 예외를 처리한다.
     *
     * @param e 잘못된 이모지 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(InvalidEmojiException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEmojiException(InvalidEmojiException e) {
        log.warn("Invalid emoji: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), "INVALID_EMOJI", LocalDateTime.now()));
    }

    /**
     * 메시지 반응을 찾을 수 없는 예외를 처리한다.
     *
     * @param e 메시지 반응 미발견 예외
     * @return 404 Not Found 응답
     */
    @ExceptionHandler(MessageReactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMessageReactionNotFoundException(MessageReactionNotFoundException e) {
        log.warn("Message reaction not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), "MESSAGE_REACTION_NOT_FOUND", LocalDateTime.now()));
    }

    /**
     * Rate Limit 초과 예외를 처리한다.
     *
     * @param e Rate Limit 초과 예외
     * @return 429 Too Many Requests 응답 (Retry-After 헤더 포함)
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(RateLimitExceededException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
                .body(new ErrorResponse(e.getMessage(), "RATE_LIMIT_EXCEEDED", LocalDateTime.now()));
    }

    /**
     * 차단 정보를 찾을 수 없는 예외를 처리한다.
     *
     * @param e 차단 정보 미발견 예외
     * @return 404 Not Found 응답
     */
    @ExceptionHandler(BlockNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBlockNotFoundException(BlockNotFoundException e) {
        log.warn("Block not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), "BLOCK_NOT_FOUND", LocalDateTime.now()));
    }

    /**
     * 유효하지 않은 차단 요청 예외를 처리한다.
     *
     * @param e 유효하지 않은 차단 요청 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(InvalidBlockException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBlockException(InvalidBlockException e) {
        log.warn("Invalid block request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), "INVALID_BLOCK", LocalDateTime.now()));
    }

    /**
     * 신고 정보를 찾을 수 없는 예외를 처리한다.
     *
     * @param e 신고 정보 미발견 예외
     * @return 404 Not Found 응답
     */
    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReportNotFoundException(ReportNotFoundException e) {
        log.warn("Report not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), "REPORT_NOT_FOUND", LocalDateTime.now()));
    }

    /**
     * 유효하지 않은 신고 요청 예외를 처리한다.
     *
     * @param e 유효하지 않은 신고 요청 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(InvalidReportException.class)
    public ResponseEntity<ErrorResponse> handleInvalidReportException(InvalidReportException e) {
        log.warn("Invalid report request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), "INVALID_REPORT", LocalDateTime.now()));
    }

    /**
     * 분산락 획득 실패 예외를 처리한다.
     *
     * @param e 분산락 획득 실패 예외
     * @return 503 Service Unavailable 응답
     */
    @ExceptionHandler(DistributedLockException.class)
    public ResponseEntity<ErrorResponse> handleDistributedLockException(DistributedLockException e) {
        log.error("Distributed lock failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("일시적으로 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해주세요.", "SERVICE_UNAVAILABLE", LocalDateTime.now()));
    }

    /**
     * 도메인 예외를 처리한다.
     *
     * @param e 도메인 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException e) {
        log.warn("Domain exception: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), "BAD_REQUEST", LocalDateTime.now()));
    }

    /**
     * 유효성 검사 예외를 처리한다.
     *
     * @param e 유효성 검사 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("유효성 검사 실패");

        log.warn("Validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message, "VALIDATION_ERROR", LocalDateTime.now()));
    }

    /**
     * 필수 요청 파라미터 누락 예외를 처리한다.
     *
     * @param e 필수 파라미터 누락 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String message = "필수 파라미터 '" + e.getParameterName() + "'이(가) 누락되었습니다.";
        log.warn("Missing parameter: {}", e.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message, "MISSING_PARAMETER", LocalDateTime.now()));
    }

    /**
     * 잘못된 인자 예외를 처리한다.
     *
     * @param e 잘못된 인자 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), "INVALID_ARGUMENT", LocalDateTime.now()));
    }

    /**
     * 예상하지 못한 예외를 처리한다.
     *
     * @param e 예외
     * @return 500 Internal Server Error 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("서버 내부 오류가 발생했습니다.", "INTERNAL_ERROR", LocalDateTime.now()));
    }

    /**
     * 에러 응답 DTO.
     *
     * @param error 에러 메시지
     * @param code 에러 코드
     * @param timestamp 발생 시각
     */
    public record ErrorResponse(
            String error,
            String code,
            LocalDateTime timestamp
    ) {}
}
