package com.cotalk.domain.port.outbound;

/**
 * 문자열 암호화/복호화를 위한 포트 인터페이스.
 * Domain 레이어에서 인프라스트럭처의 암호화 서비스에 의존하지 않도록
 * 포트를 통해 간접적으로 접근한다.
 *
 * @author seunggu.lee
 */
public interface EncryptionPort {

    /**
     * 평문을 암호화한다.
     *
     * @param plainText 암호화할 평문
     * @return 암호화된 문자열
     */
    String encrypt(String plainText);

    /**
     * 암호문을 복호화한다.
     *
     * @param encryptedText 복호화할 암호문
     * @return 복호화된 평문
     */
    String decrypt(String encryptedText);
}
