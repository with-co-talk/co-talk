package com.cotalk.infrastructure.persistence.converter;

import com.cotalk.domain.model.Email;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Email 값 객체와 데이터베이스 문자열 간의 변환을 수행하는 JPA AttributeConverter.
 * <p>
 * autoApply를 true로 설정하여, {@link Email} 타입의 모든 JPA 엔티티 필드에 자동 적용된다.
 * 이를 통해 도메인 엔티티가 인프라스트럭처 레이어의 컨버터를 직접 참조하지 않아도 된다.
 * </p>
 *
 * @author seunggu.lee
 */
@Converter(autoApply = true)
public class EmailConverter implements AttributeConverter<Email, String> {

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
     *
     * @param value 데이터베이스에 저장된 이메일 주소 문자열
     * @return Email 값 객체, null이면 null 반환
     */
    @Override
    public Email convertToEntityAttribute(String value) {
        return value == null ? null : new Email(value);
    }
}
