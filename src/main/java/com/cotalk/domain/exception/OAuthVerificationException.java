package com.cotalk.domain.exception;

/**
 * OAuth 제공자 토큰 서버 검증에 실패했을 때 발생하는 예외.
 *
 * <p>토큰 만료/위조, 서명 불일치, {@code aud}/{@code iss} 불일치, 제공자 API 호출 실패 등
 * 검증 단계의 모든 실패를 표현한다. {@link HttpStatusHint#UNAUTHORIZED 401}로 매핑되어
 * 인증 실패로 응답된다.</p>
 *
 * @author seunggu.lee
 */
public class OAuthVerificationException extends DomainException {

    /**
     * 메시지를 지정하여 예외를 생성한다.
     *
     * @param message 실패 사유 메시지
     */
    public OAuthVerificationException(String message) {
        super(message, "OAUTH_VERIFICATION_FAILED", HttpStatusHint.UNAUTHORIZED);
    }

    /**
     * 메시지와 원인을 지정하여 예외를 생성한다.
     *
     * @param message 실패 사유 메시지
     * @param cause   근본 원인 예외
     */
    public OAuthVerificationException(String message, Throwable cause) {
        super(message, "OAUTH_VERIFICATION_FAILED", HttpStatusHint.UNAUTHORIZED, cause);
    }
}
