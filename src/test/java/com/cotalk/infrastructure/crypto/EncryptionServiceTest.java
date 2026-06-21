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
                AppProperties.Search.of("dGVzdC1ibGluZC1pbmRleC1zZWNyZXQtZm9yLXVuaXQtdGVzdHM="),
                new AppProperties.Lock(false)
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

    @Test
    @DisplayName("should_평문_그대로_반환_when_암호화_비활성화")
    void should_returnPlaintext_when_disabled() {
        // given: 암호화 비활성화
        AppProperties appProperties = createTestAppProperties(TEST_KEY, false);
        EncryptionService disabled = new EncryptionService(appProperties);

        // when & then: fail-open으로 평문 그대로 반환 (운영 prod는 enabled=true 강제이므로 영향 없음)
        assertThat(disabled.isEnabled()).isFalse();
        assertThat(disabled.encrypt("평문 메시지")).isEqualTo("평문 메시지");
        assertThat(disabled.decrypt("평문 메시지")).isEqualTo("평문 메시지");
    }

    @Test
    @DisplayName("should_경고없이_정상_when_비활성화_test_프로파일")
    void should_notFail_when_disabledInTestProfile() {
        // given: 비활성화 + test 프로파일
        AppProperties appProperties = createTestAppProperties(TEST_KEY, false);
        org.springframework.mock.env.MockEnvironment env = new org.springframework.mock.env.MockEnvironment();
        env.setActiveProfiles("test");
        EncryptionService service = new EncryptionService(appProperties, env);

        // when & then: warnIfDisabled가 test 프로파일에서는 조용히 통과
        org.assertj.core.api.Assertions.assertThatCode(service::warnIfDisabled)
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should_경고로그_조건_충족_when_비활성화_비test_프로파일")
    void should_warn_when_disabledInNonTestProfile() {
        // given: 비활성화 + dev(비-test) 프로파일
        AppProperties appProperties = createTestAppProperties(TEST_KEY, false);
        org.springframework.mock.env.MockEnvironment env = new org.springframework.mock.env.MockEnvironment();
        env.setActiveProfiles("dev");
        EncryptionService service = new EncryptionService(appProperties, env);

        // when & then: 비-test 프로파일에서도 예외 없이 WARN 경로를 수행
        org.assertj.core.api.Assertions.assertThatCode(service::warnIfDisabled)
                .doesNotThrowAnyException();
    }
}
