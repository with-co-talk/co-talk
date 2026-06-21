package com.cotalk.infrastructure.exception;

import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.exception.InvalidCredentialsException;
import com.cotalk.domain.exception.RateLimitExceededException;
import com.cotalk.infrastructure.lock.DistributedLockException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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
     * 프레임워크 내부 영문 메시지를 가릴 때 사용하는 고정 한국어 메시지.
     */
    private static final String DEFAULT_INVALID_REQUEST_MESSAGE = "요청 값이 올바르지 않습니다.";

    /**
     * 예외 메시지를 클라이언트에 노출하기 전 위생 처리한다.
     *
     * <p>도메인/애플리케이션 계층이 의도적으로 던지는 메시지는 한국어(한글 포함)이므로
     * 그대로 노출하고, 한글이 전혀 없는 메시지(프레임워크/JDK 영문 내부 메시지)는
     * 정보 노출 방지를 위해 고정 한국어 메시지로 치환한다.</p>
     *
     * @param rawMessage 원본 예외 메시지 (null 가능)
     * @return 노출 가능한 메시지
     */
    private static String sanitizeClientMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank() || !containsHangul(rawMessage)) {
            return DEFAULT_INVALID_REQUEST_MESSAGE;
        }
        return rawMessage;
    }

    /**
     * 문자열에 한글 음절이 포함되어 있는지 확인한다.
     *
     * @param text 검사할 문자열
     * @return 한글 포함 여부
     */
    private static boolean containsHangul(String text) {
        return text.codePoints().anyMatch(cp -> cp >= 0xAC00 && cp <= 0xD7A3);
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
                .body(new ErrorResponse(e.getMessage(), e.getErrorCode(), LocalDateTime.now()));
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
                .body(new ErrorResponse("이메일 또는 비밀번호가 올바르지 않습니다.", e.getErrorCode(), LocalDateTime.now()));
    }

    /**
     * 도메인 예외를 통합 처리한다.
     * 각 예외에 내장된 errorCode와 statusHint를 사용하여 적절한 HTTP 응답을 생성한다.
     *
     * @param e 도메인 예외
     * @return 예외의 statusHint에 해당하는 HTTP 상태 응답
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException e) {
        log.warn("Domain exception [{}]: {}", e.getErrorCode(), e.getMessage());
        HttpStatus status = HttpStatus.resolve(e.getStatusHint().getStatusCode());
        // HttpStatusHint에 정의되지 않은 코드가 들어오면 null → 400으로 처리. (향후 500 정책으로 변경 가능)
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status)
                .body(new ErrorResponse(e.getMessage(), e.getErrorCode(), LocalDateTime.now()));
    }

    /**
     * 데이터 무결성 위반 예외를 처리한다.
     *
     * <p>유니크 제약(예: 이메일/닉네임 중복) 위반 등 DB 무결성 위반이 도메인 예외로
     * 사전에 걸러지지 않고 영속 계층까지 도달한 경우를 위생 처리한다. 프레임워크/JDBC
     * 내부 영문 메시지와 스택 정보를 노출하지 않고 고정 한국어 메시지로 409를 반환한다.</p>
     *
     * @param e 데이터 무결성 위반 예외
     * @return 409 Conflict 응답 (위생 처리된 메시지)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("데이터 무결성 위반: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("이미 존재하는 데이터와 충돌합니다.", "DATA_INTEGRITY_VIOLATION", LocalDateTime.now()));
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
     * <p>도메인/애플리케이션 계층이 의도적으로 던지는 한국어 메시지는 그대로 노출하지만,
     * Spring/JDK 등 프레임워크 내부에서 발생하는 영문 메시지(예: 음수 page에 대한
     * {@code "Page index must not be less than zero"})는 정보 노출 방지를 위해
     * 고정 한국어 메시지로 치환한다. prod의 {@code include-message: never} 설정과
     * 일관되도록 한다.</p>
     *
     * @param e 잘못된 인자 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        String message = sanitizeClientMessage(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message, "INVALID_ARGUMENT", LocalDateTime.now()));
    }

    /**
     * 요청 본문 파싱 실패 예외를 처리한다.
     *
     * @param e 요청 본문 파싱 실패 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("요청 본문 파싱 실패: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("요청 본문을 읽을 수 없습니다.", "INVALID_REQUEST_BODY", LocalDateTime.now()));
    }

    /**
     * 제약 조건 위반 예외를 처리한다.
     *
     * @param e 제약 조건 위반 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        // e.getMessage()에는 "method.param: must be greater than or equal to 0" 같은
        // 프레임워크 내부 영문 상세가 포함되므로 클라이언트에 그대로 노출하지 않는다.
        log.warn("제약 조건 위반: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(DEFAULT_INVALID_REQUEST_MESSAGE, "CONSTRAINT_VIOLATION", LocalDateTime.now()));
    }

    /**
     * 지원하지 않는 HTTP 메서드 예외를 처리한다.
     *
     * @param e 지원하지 않는 HTTP 메서드 예외
     * @return 405 Method Not Allowed 응답
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("지원하지 않는 HTTP 메서드: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse("지원하지 않는 HTTP 메서드입니다.", "METHOD_NOT_ALLOWED", LocalDateTime.now()));
    }

    /**
     * 지원하지 않는 Content-Type 예외를 처리한다.
     *
     * @param e 지원하지 않는 Content-Type 예외
     * @return 415 Unsupported Media Type 응답
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("지원하지 않는 Content-Type: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse("지원하지 않는 Content-Type입니다.", "UNSUPPORTED_MEDIA_TYPE", LocalDateTime.now()));
    }

    /**
     * 접근 거부 예외를 처리한다.
     *
     * @param e 접근 거부 예외
     * @return 403 Forbidden 응답
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        log.warn("접근 거부: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("접근 권한이 없습니다.", "ACCESS_DENIED", LocalDateTime.now()));
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
