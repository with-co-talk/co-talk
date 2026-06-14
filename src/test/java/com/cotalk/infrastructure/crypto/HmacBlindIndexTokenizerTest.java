package com.cotalk.infrastructure.crypto;

import com.cotalk.infrastructure.config.properties.AppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link HmacBlindIndexTokenizer} 단위 테스트.
 *
 * <p>정규화, 글자 단위 3-gram 추출, HMAC 결정성, 한국어/이모지 코드포인트 처리,
 * 길이 가드(3글자 미만)를 검증한다.</p>
 */
@DisplayName("HmacBlindIndexTokenizer")
class HmacBlindIndexTokenizerTest {

    // 고정 더미 시크릿 (Base64) — 결정성 검증용
    private static final String SECRET = "dGVzdC1ibGluZC1pbmRleC1zZWNyZXQtZm9yLXVuaXQtdGVzdHM=";

    private HmacBlindIndexTokenizer tokenizer() {
        AppProperties props = appPropertiesWithSecret(SECRET);
        return new HmacBlindIndexTokenizer(props);
    }

    private AppProperties appPropertiesWithSecret(String secret) {
        return new AppProperties(
                "http://localhost:3000",
                new AppProperties.Cors("http://localhost:3000"),
                new AppProperties.Redis("chat:room:", "user:event:"),
                new AppProperties.PasswordReset(30),
                new AppProperties.Terms("1.0", "1.0"),
                new AppProperties.Encryption("dGhpc2lzYXRlc3RrZXlmb3JkZXZlbG9wbWVudG9ubHk=", false),
                new AppProperties.Swagger("http://localhost:8080", "API 서버"),
                new AppProperties.Search(secret)
        );
    }

    @Test
    @DisplayName("3글자 이상 한국어를 글자 단위 트라이그램 개수만큼 토큰화한다")
    void should_produceTrigramTokens_when_koreanTextGiven() {
        Set<String> tokens = tokenizer().tokenize("비밀번호");
        // "비밀번호" → [비밀번, 밀번호] = 2개
        assertThat(tokens).hasSize(2);
    }

    @Test
    @DisplayName("동일 입력에 대해 결정적(deterministic)으로 같은 토큰을 생성한다")
    void should_beDeterministic_when_sameInput() {
        HmacBlindIndexTokenizer t1 = tokenizer();
        HmacBlindIndexTokenizer t2 = tokenizer();
        assertThat(t1.tokenize("안녕하세요")).isEqualTo(t2.tokenize("안녕하세요"));
    }

    @Test
    @DisplayName("시크릿이 다르면 같은 평문이라도 다른 토큰을 생성한다 (HMAC 키 의존)")
    void should_differ_when_secretDiffers() {
        HmacBlindIndexTokenizer t1 = tokenizer();
        HmacBlindIndexTokenizer t2 = new HmacBlindIndexTokenizer(
                appPropertiesWithSecret("YW5vdGhlci1zZWNyZXQtdmFsdWUtZm9yLXRoZS10ZXN0LWNhc2U="));
        assertThat(t1.tokenize("비밀번호")).isNotEqualTo(t2.tokenize("비밀번호"));
    }

    @Test
    @DisplayName("키워드 토큰화 결과는 본문 토큰화 결과의 부분집합이다 (부분일치 매칭 성립)")
    void should_keywordTokensBeSubsetOfContentTokens() {
        HmacBlindIndexTokenizer t = tokenizer();
        Set<String> content = t.tokenize("오늘 비밀번호를 변경했어요");
        Set<String> query = t.tokenizeQuery("비밀번호");
        assertThat(query).isNotEmpty();
        assertThat(content).containsAll(query);
    }

    @Test
    @DisplayName("대소문자/공백/유니코드 정규화가 토큰화에 반영된다")
    void should_normalize_caseAndWhitespace() {
        HmacBlindIndexTokenizer t = tokenizer();
        // "Hello" 와 "h e l l o" 는 정규화(소문자+공백제거) 후 같은 시퀀스
        assertThat(t.tokenize("Hello")).isEqualTo(t.tokenize("h e l l o"));
    }

    @Test
    @DisplayName("NFC 정규화로 분해형/완성형 한글이 같은 토큰을 만든다")
    void should_normalizeNfc_when_decomposedHangul() {
        HmacBlindIndexTokenizer t = tokenizer();
        String composed = Normalizer.normalize("가나다", Normalizer.Form.NFC);
        String decomposed = Normalizer.normalize("가나다", Normalizer.Form.NFD);
        assertThat(decomposed).isNotEqualTo(composed); // 입력은 다름
        assertThat(t.tokenize(decomposed)).isEqualTo(t.tokenize(composed)); // 토큰은 같음
    }

    @Test
    @DisplayName("이모지(서로게이트 페어)를 코드포인트 단위로 처리한다")
    void should_handleEmoji_asCodePoints() {
        HmacBlindIndexTokenizer t = tokenizer();
        // 이모지 3개 → 1개 트라이그램 (서로게이트 분리 없이 글자 3개로 인식)
        Set<String> tokens = t.tokenize("😀😁😂");
        assertThat(tokens).hasSize(1);
    }

    @Test
    @DisplayName("3글자 미만은 빈 토큰 집합을 반환한다")
    void should_returnEmpty_when_fewerThanThreeChars() {
        HmacBlindIndexTokenizer t = tokenizer();
        assertThat(t.tokenize("가")).isEmpty();
        assertThat(t.tokenize("가나")).isEmpty();
        assertThat(t.tokenizeQuery("ab")).isEmpty();
    }

    @Test
    @DisplayName("null/빈 문자열은 빈 토큰 집합을 반환한다")
    void should_returnEmpty_when_nullOrBlank() {
        HmacBlindIndexTokenizer t = tokenizer();
        assertThat(t.tokenize(null)).isEmpty();
        assertThat(t.tokenize("")).isEmpty();
        assertThat(t.tokenize("   ")).isEmpty();
    }

    @Test
    @DisplayName("토큰 길이는 VARCHAR(24) 이내다")
    void should_produceTokensWithinColumnLength() {
        HmacBlindIndexTokenizer t = tokenizer();
        assertThat(t.tokenize("비밀번호변경")).allSatisfy(tok ->
                assertThat(tok.length()).isLessThanOrEqualTo(24));
    }

    @Test
    @DisplayName("normalize는 소문자+공백제거+NFC를 적용한다")
    void should_normalizeConsistently() {
        HmacBlindIndexTokenizer t = tokenizer();
        assertThat(t.normalize("  Hello World  ")).isEqualTo("helloworld");
        assertThat(t.normalize(null)).isEmpty();
    }

    @Test
    @DisplayName("시크릿이 비어있으면 생성 시 예외를 던진다")
    void should_throw_when_secretMissing() {
        assertThatThrownBy(() -> new HmacBlindIndexTokenizer(appPropertiesWithSecret("")))
                .isInstanceOf(IllegalStateException.class);
    }
}
