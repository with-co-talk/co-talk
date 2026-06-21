package com.cotalk.infrastructure.crypto;

import com.cotalk.infrastructure.config.properties.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM 암호화/복호화 서비스.
 * 메시지 내용 등 민감한 데이터를 암호화하여 DB에 저장한다.
 *
 * <p>GCM 모드는 인증된 암호화를 제공하여 데이터 무결성도 보장한다.</p>
 *
 * <p>주의: 암호화를 활성화하면 DB 레벨 검색(LIKE)이 불가능해진다.
 * 검색 기능이 필요한 경우 {@code app.encryption.enabled=false}로 설정한다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey;
    private final boolean enabled;
    private final Environment environment;

    /**
     * EncryptionService 생성자.
     *
     * @param appProperties 앱 설정 프로퍼티
     * @param environment 활성 프로파일 확인용 스프링 환경
     */
    @Autowired
    public EncryptionService(AppProperties appProperties, Environment environment) {
        this.environment = environment;
        this.enabled = appProperties.encryption().enabled();
        if (this.enabled) {
            String encryptionKey = appProperties.encryption().key();
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            if (keyBytes.length != 32) {
                throw new IllegalArgumentException("암호화 키는 32바이트(256비트)여야 합니다.");
            }
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } else {
            this.secretKey = null;
        }
    }

    /**
     * EncryptionService 생성자 (Environment 미지정).
     * 단위 테스트 등 프로파일 확인이 불필요한 경우에 사용한다.
     *
     * @param appProperties 앱 설정 프로퍼티
     */
    public EncryptionService(AppProperties appProperties) {
        this(appProperties, null);
    }

    /**
     * 암호화가 비활성화된 상태로 비-test 프로파일이 활성화되면 시작 시점에 WARN 로그를 남긴다.
     *
     * <p>{@code enabled=false}이면 평문이 그대로 저장/반환되므로(fail-open),
     * 프로파일/환경변수 설정 실수로 운영 데이터가 평문 노출되는 상황을 가시화한다.
     * 운영(prod)은 {@code enabled=true}를 강제하므로 영향받지 않는다.</p>
     */
    @PostConstruct
    void warnIfDisabled() {
        if (enabled || environment == null) {
            return;
        }
        boolean testProfile = environment.acceptsProfiles(Profiles.of("test"));
        if (!testProfile) {
            log.warn("암호화(app.encryption.enabled)가 비활성화되어 있습니다. "
                    + "메시지 등 민감 데이터가 평문으로 저장됩니다. "
                    + "운영/스테이징 환경이라면 즉시 설정을 점검하세요. activeProfiles={}",
                    Arrays.toString(environment.getActiveProfiles()));
        }
    }

    /**
     * 암호화 활성화 여부를 반환한다.
     *
     * @return 암호화가 활성화되어 있으면 true
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 평문을 AES-256-GCM으로 암호화한다.
     *
     * @param plainText 암호화할 평문
     * @return Base64로 인코딩된 암호문 (IV + 암호문), 암호화 비활성화 시 평문 반환
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        if (!enabled) {
            return plainText;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + 암호문을 합쳐서 반환
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new EncryptionException("암호화 실패", e);
        }
    }

    /**
     * AES-256-GCM으로 암호화된 문자열을 복호화한다.
     *
     * @param encryptedText Base64로 인코딩된 암호문 (IV + 암호문)
     * @return 복호화된 평문, 암호화 비활성화 시 입력값 그대로 반환
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        if (!enabled) {
            return encryptedText;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("복호화 실패", e);
        }
    }

    /**
     * 암호화/복호화 예외.
     */
    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
