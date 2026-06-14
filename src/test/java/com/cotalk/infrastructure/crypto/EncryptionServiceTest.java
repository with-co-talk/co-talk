package com.cotalk.infrastructure.crypto;

import com.cotalk.infrastructure.config.properties.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EncryptionService 단위 테스트.
 *
 * @author seunggu.lee
 */
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    // 테스트용 32바이트 키 (Base64 인코딩)
    private static final String TEST_KEY = Base64.getEncoder().encodeToString(
            "12345678901234567890123456789012".getBytes()
    );

    @BeforeEach
    void setUp() {
        AppProperties appProperties = createTestAppProperties(TEST_KEY, true);
        encryptionService = new EncryptionService(appProperties);
    }

    private AppProperties createTestAppProperties(String encryptionKey, boolean enabled) {
        return new AppProperties(
                "http://localhost:3000",
                new AppProperties.Cors("http://localhost:3000"),
                new AppProperties.Redis("chat:room:", "user:event:"),
                new AppProperties.PasswordReset(30),
                new AppProperties.Terms("1.0", "1.0"),
                new AppProperties.Encryption(encryptionKey, enabled),
                new AppProperties.Swagger("http://localhost:8080", "API 서버"),
                AppProperties.Search.of("dGVzdC1ibGluZC1pbmRleC1zZWNyZXQtZm9yLXVuaXQtdGVzdHM=")
        );
    }

    @Test
    @DisplayName("should_암호화_후_복호화하면_원본_반환_when_일반_텍스트")
    void should_returnOriginal_when_encryptAndDecrypt() {
        // given
        String original = "안녕하세요! Hello, World! 🎉";

        // when
        String encrypted = encryptionService.encrypt(original);
        String decrypted = encryptionService.decrypt(encrypted);

        // then
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    @DisplayName("should_다른_암호문_생성_when_같은_평문_암호화")
    void should_produceDifferentCiphertext_when_encryptingSamePlaintext() {
        // given
        String original = "테스트 메시지";

        // when
        String encrypted1 = encryptionService.encrypt(original);
        String encrypted2 = encryptionService.encrypt(original);

        // then (IV가 랜덤이므로 같은 평문도 다른 암호문 생성)
        assertThat(encrypted1).isNotEqualTo(encrypted2);

        // 둘 다 복호화하면 같은 평문
        assertThat(encryptionService.decrypt(encrypted1)).isEqualTo(original);
        assertThat(encryptionService.decrypt(encrypted2)).isEqualTo(original);
    }

    @Test
    @DisplayName("should_null_반환_when_null_입력")
    void should_returnNull_when_inputIsNull() {
        // when & then
        assertThat(encryptionService.encrypt(null)).isNull();
        assertThat(encryptionService.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("should_빈_문자열_반환_when_빈_문자열_입력")
    void should_returnEmpty_when_inputIsEmpty() {
        // when & then
        assertThat(encryptionService.encrypt("")).isEmpty();
        assertThat(encryptionService.decrypt("")).isEmpty();
    }

    @Test
    @DisplayName("should_긴_텍스트_암호화_성공_when_4000자_메시지")
    void should_encryptSuccessfully_when_longText() {
        // given
        String longText = "A".repeat(4000);

        // when
        String encrypted = encryptionService.encrypt(longText);
        String decrypted = encryptionService.decrypt(encrypted);

        // then
        assertThat(decrypted).isEqualTo(longText);
    }

    @Test
    @DisplayName("should_예외_발생_when_잘못된_키_길이")
    void should_throwException_when_invalidKeyLength() {
        // given
        String shortKey = Base64.getEncoder().encodeToString("short".getBytes());
        AppProperties appProperties = createTestAppProperties(shortKey, true);

        // when & then
        assertThatThrownBy(() -> new EncryptionService(appProperties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32바이트");
    }

    @Test
    @DisplayName("should_예외_발생_when_손상된_암호문_복호화")
    void should_throwException_when_decryptingCorruptedCiphertext() {
        // given
        String corrupted = "this-is-not-valid-base64!@#$";

        // when & then
        assertThatThrownBy(() -> encryptionService.decrypt(corrupted))
                .isInstanceOf(EncryptionService.EncryptionException.class);
    }

    @Test
    @DisplayName("should_유니코드_문자_정상_처리_when_이모지_포함")
    void should_handleUnicode_when_containsEmoji() {
        // given
        String withEmoji = "메시지 🔥🎉💬 이모지 테스트";

        // when
        String encrypted = encryptionService.encrypt(withEmoji);
        String decrypted = encryptionService.decrypt(encrypted);

        // then
        assertThat(decrypted).isEqualTo(withEmoji);
    }
}
