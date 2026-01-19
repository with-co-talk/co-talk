package com.cotalk.infrastructure.security;

import org.springframework.stereotype.Component;

/**
 * HTML 이스케이프 처리를 위한 유틸리티 클래스.
 * XSS(Cross-Site Scripting) 공격을 방지하기 위해 사용자 입력을 안전하게 처리한다.
 *
 * @author seunggu.lee
 */
@Component
public class HtmlSanitizer {

    /**
     * 문자열에서 HTML 특수문자를 이스케이프 처리한다.
     * XSS 공격을 방지하기 위해 {@code <}, {@code >}, {@code &}, {@code "}, {@code '} 문자를
     * HTML 엔티티로 변환한다.
     *
     * @param input 이스케이프 처리할 입력 문자열
     * @return 이스케이프 처리된 문자열, null인 경우 빈 문자열 반환
     */
    public String sanitize(String input) {
        if (input == null) {
            return "";
        }

        StringBuilder sanitized = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&' -> sanitized.append("&amp;");
                case '<' -> sanitized.append("&lt;");
                case '>' -> sanitized.append("&gt;");
                case '"' -> sanitized.append("&quot;");
                case '\'' -> sanitized.append("&#x27;");
                default -> sanitized.append(c);
            }
        }
        return sanitized.toString();
    }

    /**
     * 문자열에서 HTML 특수문자를 이스케이프 처리한다.
     * 정적 메서드 버전.
     *
     * @param input 이스케이프 처리할 입력 문자열
     * @return 이스케이프 처리된 문자열, null인 경우 빈 문자열 반환
     */
    public static String escape(String input) {
        if (input == null) {
            return "";
        }

        StringBuilder sanitized = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&' -> sanitized.append("&amp;");
                case '<' -> sanitized.append("&lt;");
                case '>' -> sanitized.append("&gt;");
                case '"' -> sanitized.append("&quot;");
                case '\'' -> sanitized.append("&#x27;");
                default -> sanitized.append(c);
            }
        }
        return sanitized.toString();
    }
}
