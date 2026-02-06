package com.cotalk.infrastructure.util;

/**
 * 로그 출력 시 개인정보(PII)를 마스킹하기 위한 유틸리티 클래스.
 * <p>
 * 이메일 주소 등 민감한 정보가 로그에 평문으로 노출되지 않도록
 * 마스킹 처리한다.
 * </p>
 *
 * @author seunggu.lee
 */
public final class LogMaskingUtil {

    private LogMaskingUtil() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    /**
     * 이메일 주소를 마스킹한다.
     * <p>
     * 예: "user@example.com" -> "us***@example.com"
     * </p>
     *
     * @param email 마스킹할 이메일 주소
     * @return 마스킹된 이메일 주소
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIdx = email.indexOf('@');
        if (atIdx <= 2) {
            return "***" + email.substring(atIdx);
        }
        return email.substring(0, 2) + "***" + email.substring(atIdx);
    }
}
