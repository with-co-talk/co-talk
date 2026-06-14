package com.cotalk.domain.port.outbound;

import java.util.Set;

/**
 * 블라인드 인덱스 토큰화 포트.
 *
 * <p>암호화되어 저장되는 메시지 본문을 검색하기 위한 결정적(deterministic) 토큰을 생성한다.
 * 평문은 AES-GCM 랜덤 IV로 암호화되어 DB LIKE 검색이 불가능하므로, 평문의 글자 단위
 * 트라이그램(3-gram)을 HMAC-SHA256으로 변환한 고정 길이 토큰을 별도 테이블에 적재해
 * 등가매칭(exact match) 조인으로 검색을 복구한다.</p>
 *
 * <p>저장 토큰화와 검색 키워드 토큰화는 반드시 동일한 정규화/트라이그램 규칙을 사용해야
 * 일치가 보장된다. 토큰은 "후보 좁히기"일 뿐이며, 최종 정확도는 복호화 후 substring 검증
 * (2단계)에서 보증한다.</p>
 *
 * <p>HMAC 시크릿 주입이 필요하므로 구현은 infrastructure 레이어에 둔다
 * ({@code EncryptionPort}와 동일한 패턴).</p>
 *
 * @author seunggu.lee
 */
public interface BlindIndexTokenizer {

    /**
     * 저장 대상 평문 텍스트를 토큰 집합으로 변환한다.
     *
     * <p>정규화(NFC, 소문자, 공백/제어문자 제거) 후 글자(유니코드 코드포인트) 단위
     * 슬라이딩 윈도우로 트라이그램을 추출하고, 각 트라이그램을 HMAC-SHA256으로 변환해
     * 잘라낸(truncated) Base64url 토큰을 만든다. 중복은 제거한다.</p>
     *
     * @param text 토큰화할 평문 (보통 sanitized content)
     * @return 토큰 집합 (3글자 미만이거나 비어있으면 빈 집합)
     */
    Set<String> tokenize(String text);

    /**
     * 검색 키워드를 토큰 집합으로 변환한다.
     *
     * <p>{@link #tokenize(String)}와 동일한 파이프라인을 사용한다. 키워드와 본문이 같은
     * 규칙으로 토큰화되어야 토큰 AND 매칭이 성립한다.</p>
     *
     * @param keyword 검색 키워드
     * @return 토큰 집합 (3글자 미만이거나 비어있으면 빈 집합)
     */
    Set<String> tokenizeQuery(String keyword);

    /**
     * 토큰화/검색에 사용하는 것과 동일한 정규화를 적용한다.
     *
     * <p>2단계 복호화 substring 검증에서 본문과 키워드를 동일 규칙으로 정규화한 뒤
     * {@code contains} 비교를 하기 위해 노출한다. (NFC + 소문자 + 공백/제어문자 제거)</p>
     *
     * @param text 정규화할 문자열 (null이면 빈 문자열 반환)
     * @return 정규화된 문자열
     */
    String normalize(String text);
}
