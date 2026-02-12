package com.cotalk.domain.exception;

/**
 * 이메일 인증 토큰이 유효하지 않을 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class InvalidEmailVerificationTokenException extends DomainException {

    public InvalidEmailVerificationTokenException(String message) {
        super(message);
    }

    /**
     * 토큰을 찾을 수 없을 때 예외를 생성한다.
     *
     * @return InvalidEmailVerificationTokenException 인스턴스
     */
    public static InvalidEmailVerificationTokenException notFound() {
        return new InvalidEmailVerificationTokenException("유효하지 않은 이메일 인증 링크입니다.");
    }

    /**
     * 토큰이 만료되었을 때 예외를 생성한다.
     *
     * @return InvalidEmailVerificationTokenException 인스턴스
     */
    public static InvalidEmailVerificationTokenException expired() {
        return new InvalidEmailVerificationTokenException("이메일 인증 링크가 만료되었습니다.");
    }

    /**
     * 이미 인증 완료된 토큰일 때 예외를 생성한다.
     *
     * @return InvalidEmailVerificationTokenException 인스턴스
     */
    public static InvalidEmailVerificationTokenException alreadyVerified() {
        return new InvalidEmailVerificationTokenException("이미 인증이 완료된 이메일입니다.");
    }
}
