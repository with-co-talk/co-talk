package com.cotalk.infrastructure.crypto;

import com.cotalk.domain.port.outbound.BlindIndexTokenizer;
import com.cotalk.infrastructure.config.properties.AppProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * HMAC-SHA256 기반 블라인드 인덱스 토큰화 구현체.
 *
 * <p>파이프라인: NFC 정규화 → 소문자(Locale.ROOT) → 공백/제어문자 제거 →
 * 글자(유니코드 코드포인트) 단위 슬라이딩 3-gram → 각 트라이그램을 HMAC-SHA256으로 변환 →
 * 앞 12바이트로 truncate → Base64url(no padding) 인코딩 → distinct 집합.</p>
 *
 * <p>HMAC 시크릿은 {@code app.search.blind-index-secret}(Base64)에서 로드하며,
 * AES 암호화 키와 완전히 분리된다. 시크릿이 없으면 빈 토큰만 만들어 검색이 조용히 깨지는 것을
 * 방지하기 위해 생성 시점에 예외를 던진다(fail-fast).</p>
 *
 * <p>트라이그램은 엔트로피가 낮아 일반 해시(SHA-256)였다면 사전공격으로 즉시 역산되지만,
 * HMAC + 비밀키가 이를 막는다(시크릿 보안이 전체 안전성의 단일 의존점).</p>
 *
 * @author seunggu.lee
 */
@Component
public class HmacBlindIndexTokenizer implements BlindIndexTokenizer {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    /** 토큰화 최소 글자 수 (트라이그램). */
    private static final int TRIGRAM_SIZE = 3;
    /** HMAC 결과에서 토큰으로 취할 바이트 수 (truncation). 12B → base64url 16자. */
    private static final int TOKEN_BYTE_LENGTH = 12;

    private final SecretKeySpec secretKey;
    private final Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();

    /**
     * 토큰화 구현체 생성자.
     *
     * @param appProperties 앱 설정 (블라인드 인덱스 시크릿 포함)
     * @throws IllegalStateException 블라인드 인덱스 시크릿이 비어있는 경우
     */
    public HmacBlindIndexTokenizer(AppProperties appProperties) {
        String secret = appProperties.search().blindIndexSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.search.blind-index-secret이 설정되지 않았습니다. 메시지 검색 블라인드 인덱스에 필수입니다.");
        }
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
    }

    /**
     * 평문 텍스트를 토큰 집합으로 변환한다.
     *
     * @param text 토큰화할 평문
     * @return 토큰 집합 (3글자 미만/빈 입력은 빈 집합)
     */
    @Override
    public Set<String> tokenize(String text) {
        return tokenizeInternal(text);
    }

    /**
     * 검색 키워드를 토큰 집합으로 변환한다.
     *
     * @param keyword 검색 키워드
     * @return 토큰 집합 (3글자 미만/빈 입력은 빈 집합)
     */
    @Override
    public Set<String> tokenizeQuery(String keyword) {
        return tokenizeInternal(keyword);
    }

    /**
     * 토큰화/검색에 사용하는 것과 동일한 정규화를 적용한다.
     *
     * @param text 정규화할 문자열
     * @return 정규화된 문자열 (null이면 빈 문자열)
     */
    @Override
    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        String nfc = Normalizer.normalize(text, Normalizer.Form.NFC);
        String lowered = nfc.toLowerCase(Locale.ROOT);
        // 모든 공백/제어문자 제거 (한국어 띄어쓰기 불규칙성 → 부분일치 UX 우선)
        StringBuilder sb = new StringBuilder(lowered.length());
        lowered.codePoints().forEach(cp -> {
            if (!Character.isWhitespace(cp) && !Character.isISOControl(cp)) {
                sb.appendCodePoint(cp);
            }
        });
        return sb.toString();
    }

    private Set<String> tokenizeInternal(String text) {
        String normalized = normalize(text);
        // 코드포인트 배열로 변환하여 서로게이트 페어(이모지)를 한 글자로 취급
        int[] codePoints = normalized.codePoints().toArray();
        if (codePoints.length < TRIGRAM_SIZE) {
            return Set.of();
        }

        Set<String> tokens = new LinkedHashSet<>();
        for (int i = 0; i + TRIGRAM_SIZE <= codePoints.length; i++) {
            int[] window = Arrays.copyOfRange(codePoints, i, i + TRIGRAM_SIZE);
            String trigram = new String(window, 0, window.length);
            tokens.add(hmacToken(trigram));
        }
        return tokens;
    }

    private String hmacToken(String trigram) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);
            byte[] full = mac.doFinal(trigram.getBytes(StandardCharsets.UTF_8));
            byte[] truncated = Arrays.copyOf(full, TOKEN_BYTE_LENGTH);
            return urlEncoder.encodeToString(truncated);
        } catch (Exception e) {
            throw new IllegalStateException("블라인드 인덱스 토큰 생성 실패", e);
        }
    }
}
