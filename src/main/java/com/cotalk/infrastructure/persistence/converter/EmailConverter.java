package com.cotalk.infrastructure.persistence.converter;

import com.cotalk.domain.model.Email;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Email 값 객체와 데이터베이스 문자열 간의 변환을 수행하는 JPA AttributeConverter.
 * <p>
 * autoApply를 true로 설정하여, {@link Email} 타입의 모든 JPA 엔티티 필드에 자동 적용된다.
 * 이를 통해 도메인 엔티티가 인프라스트럭처 레이어의 컨버터를 직접 참조하지 않아도 된다.
 * </p>
 * <p>
 * DB에 형식이 잘못된 레거시 값이 있으면 {@link Email} 생성 시 {@link IllegalArgumentException}이 발생하며,
 * {@link com.cotalk.infrastructure.exception.GlobalExceptionHandler}에서 400 Bad Request로 매핑된다.
 * 형식 오류 시 마스킹된 값만 로그에 남긴다.
 * </p>
 *
 * @author seunggu.lee
 */
@Converter(autoApply = true)
public class EmailConverter implements AttributeConverter<Email, String> {

    private static final Logger log = LoggerFactory.getLogger(EmailConverter.class);

    /**
     * 이메일 값을 로그용으로 마스킹한다. (앞 2자 + ***@*** + 도메인 마지막 부분)
     */
    private static String maskForLog(String value) {
        if (value == null || value.length() < 3) {
            return "***";
        }
        int at = value.indexOf('@');
        if (at <= 0 || at >= value.length() - 1) {
            return value.substring(0, Math.min(2, value.length())) + "***";
        }
        String local = value.substring(0, Math.min(2, at)) + "***";
        String domain = value.substring(at + 1);
        int lastDot = domain.lastIndexOf('.');
        String domainSuffix = lastDot > 0 ? domain.substring(lastDot) : "";
        return local + "@***" + domainSuffix;
    }

    /**
     * Email 값 객체를 데이터베이스 컬럼 값(String)으로 변환한다.
     *
     * @param email 변환할 Email 값 객체
     * @return 이메일 주소 문자열, null이면 null 반환
     */
    @Override
    public String convertToDatabaseColumn(Email email) {
        return email == null ? null : email.value();
    }

    /**
     * 데이터베이스 컬럼 값(String)을 Email 값 객체로 변환한다.
     * 형식이 잘못된 레거시 값이 있으면 IllegalArgumentException이 발생하며, 마스킹된 값만 로그에 남긴다.
     *
     * @param value 데이터베이스에 저장된 이메일 주소 문자열
     * @return Email 값 객체, null이면 null 반환
     */
    @Override
    public Email convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new Email(value);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid email format in DB (masked): {}", maskForLog(value));
            throw e;
        }
    }
}
