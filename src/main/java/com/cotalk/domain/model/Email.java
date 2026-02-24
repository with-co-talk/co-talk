package com.cotalk.domain.model;

import java.util.regex.Pattern;

/**
 * 이메일 주소를 나타내는 값 객체.
 * 생성 시 이메일 형식 유효성을 검증한다.
 * <p>
 * 의도적으로 단순한 형식만 허용한다 (RFC 5322 전체가 아님).
 * 실무에서 대부분의 유효한 주소는 통과하며, 과도하게 복잡한 정규식을 피한 선택이다.
 * </p>
 * <p>
 * Effective Java Item 50: 방어적 복사를 적용하여
 * 생성자에서 불변식(이메일 형식)을 검증한다.
 * </p>
 *
 * @param value 이메일 주소 문자열
 * @author seunggu.lee
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * Email 값 객체를 생성한다.
     *
     * @param value 이메일 주소 문자열
     * @throws IllegalArgumentException 이메일 형식이 올바르지 않은 경우
     */
    public Email {
        if (value == null || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
