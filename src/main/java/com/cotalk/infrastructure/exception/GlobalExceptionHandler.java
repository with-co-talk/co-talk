package com.cotalk.infrastructure.exception;

import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.exception.InvalidCredentialsException;
import com.cotalk.domain.exception.RateLimitExceededException;
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
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status)
                .body(new ErrorResponse(e.getMessage(), e.getErrorCode(), LocalDateTime.now()));
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
