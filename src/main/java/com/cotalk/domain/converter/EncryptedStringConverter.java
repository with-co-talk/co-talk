package com.cotalk.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 문자열 암호화를 위한 JPA AttributeConverter.
 * 엔티티 필드에 적용하면 DB 저장 시 자동으로 암호화되고, 조회 시 자동으로 복호화된다.
 *
 * <p>사용 예:</p>
 * <pre>
 * {@code
 * @Convert(converter = EncryptedStringConverter.class)
 * private String content;
 * }
 * </pre>
 *
 * @author seunggu.lee
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    /**
     * 엔티티 속성을 DB 컬럼 값으로 변환한다 (암호화).
     *
     * @param attribute 암호화할 평문
     * @return 암호화된 문자열
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return EncryptionPortHolder.getEncryptionPort().encrypt(attribute);
        } catch (IllegalStateException _) {
            // 테스트 환경 등에서 EncryptionPort가 초기화되지 않은 경우 평문 반환
            return attribute;
        }
    }

    /**
     * DB 컬럼 값을 엔티티 속성으로 변환한다 (복호화).
     *
     * @param dbData 복호화할 암호문
     * @return 복호화된 평문
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return EncryptionPortHolder.getEncryptionPort().decrypt(dbData);
        } catch (RuntimeException _) {
            // 테스트 환경 등 EncryptionPort 미초기화, 또는 암호화되지 않은 기존 데이터(마이그레이션 호환) 시 그대로 반환
            return dbData;
        }
    }
}
