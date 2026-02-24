package com.cotalk.domain.util;

/**
 * FCM·디바이스 토큰 등 로그 출력 시 마스킹하기 위한 유틸리티 클래스.
 * <p>
 * 토큰이 로그에 평문으로 노출되지 않도록 앞 6자 + "..." + 뒤 4자 형식으로 변환한다.
 * </p>
 *
 * @author seunggu.lee
 */
public final class TokenMasker {

    private TokenMasker() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    /**
     * 토큰을 마스킹하여 로그에 안전하게 출력한다.
     * 앞 6자 + "..." + 뒤 4자 형식으로 변환한다.
     *
     * @param token 마스킹할 토큰 (null 가능)
     * @return 마스킹된 토큰 문자열 (null·길이 10 이하는 "***")
     */
    public static String mask(String token) {
        if (token == null || token.length() <= 10) {
            return "***";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }
}
