package com.cotalk.infrastructure.crypto;

import com.cotalk.domain.converter.EncryptionPortHolder;
import com.cotalk.domain.port.outbound.EncryptionPort;
import org.springframework.stereotype.Component;

/**
 * JPA Converter에서 EncryptionService에 접근하기 위한 홀더 클래스.
 * JPA Converter는 Spring이 아닌 JPA가 인스턴스화하므로 직접 의존성 주입이 불가능하다.
 * 이 클래스를 통해 정적으로 EncryptionService에 접근한다.
 *
 * @author seunggu.lee
 */
@Component
public class EncryptionServiceHolder {

    private static EncryptionService encryptionService;

    /**
     * EncryptionServiceHolder 생성자.
     * Spring이 이 컴포넌트를 생성할 때 EncryptionService를 정적 필드에 저장한다.
     * 동시에 Domain layer의 EncryptionPortHolder에도 포트 구현체를 설정한다.
     *
     * @param encryptionService 암호화 서비스
     */
    public EncryptionServiceHolder(EncryptionService encryptionService) {
        EncryptionServiceHolder.encryptionService = encryptionService;

        // Domain layer의 EncryptionPortHolder에도 설정
        EncryptionPortHolder.setEncryptionPort(new EncryptionPort() {
            @Override
            public String encrypt(String plainText) {
                return encryptionService.encrypt(plainText);
            }

            @Override
            public String decrypt(String encryptedText) {
                return encryptionService.decrypt(encryptedText);
            }
        });
    }

    /**
     * EncryptionService 인스턴스를 반환한다.
     *
     * @return EncryptionService 인스턴스
     * @throws IllegalStateException EncryptionService가 초기화되지 않은 경우
     */
    public static EncryptionService getEncryptionService() {
        if (encryptionService == null) {
            throw new IllegalStateException("EncryptionService가 초기화되지 않았습니다.");
        }
        return encryptionService;
    }
}
