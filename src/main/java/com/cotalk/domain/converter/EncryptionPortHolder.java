package com.cotalk.domain.converter;

import com.cotalk.domain.port.outbound.EncryptionPort;

/**
 * JPA Converter에서 EncryptionPort에 접근하기 위한 홀더 클래스.
 * JPA Converter는 Spring이 아닌 JPA가 인스턴스화하므로 직접 의존성 주입이 불가능하다.
 * 이 클래스를 통해 정적으로 EncryptionPort에 접근한다.
 *
 * @author seunggu.lee
 */
public final class EncryptionPortHolder {

    private static EncryptionPort encryptionPort;

    private EncryptionPortHolder() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    /**
     * EncryptionPort를 설정한다.
     *
     * @param port 암호화 포트 구현체
     */
    public static void setEncryptionPort(EncryptionPort port) {
        EncryptionPortHolder.encryptionPort = port;
    }

    /**
     * EncryptionPort 인스턴스를 반환한다.
     *
     * @return EncryptionPort 인스턴스
     * @throws IllegalStateException EncryptionPort가 초기화되지 않은 경우
     */
    public static EncryptionPort getEncryptionPort() {
        if (encryptionPort == null) {
            throw new IllegalStateException("EncryptionPort가 초기화되지 않았습니다.");
        }
        return encryptionPort;
    }
}
