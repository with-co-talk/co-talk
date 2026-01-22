package com.cotalk.domain.util;

import org.springframework.web.util.HtmlUtils;

/**
 * HTML 특수문자 이스케이프를 위한 유틸리티 클래스.
 * XSS(Cross-Site Scripting) 공격을 방지하기 위해 사용자 입력에서
 * 위험한 HTML 문자를 안전한 문자로 변환한다.
 *
 * <p>이스케이프 대상 문자:</p>
 * <ul>
 *     <li>{@code <} → {@code &lt;}</li>
 *     <li>{@code >} → {@code &gt;}</li>
 *     <li>{@code &} → {@code &amp;}</li>
 *     <li>{@code "} → {@code &quot;}</li>
 *     <li>{@code '} → {@code &#39;}</li>
 * </ul>
 *
 * @author seunggu.lee
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
        // 유틸리티 클래스는 인스턴스화할 수 없음
    }

    /**
     * HTML 특수문자를 이스케이프한다.
     * null 입력은 null을 반환한다.
     *
     * @param input 이스케이프할 문자열
     * @return 이스케이프된 문자열, 입력이 null이면 null
     */
    public static String escape(String input) {
        if (input == null) {
            return null;
        }
        return HtmlUtils.htmlEscape(input);
    }

    /**
     * HTML 특수문자를 이스케이프한다.
     * null 또는 빈 문자열 입력은 기본값을 반환한다.
     *
     * @param input        이스케이프할 문자열
     * @param defaultValue null 또는 빈 문자열일 때 반환할 기본값
     * @return 이스케이프된 문자열 또는 기본값
     */
    public static String escapeOrDefault(String input, String defaultValue) {
        if (input == null || input.isBlank()) {
            return defaultValue;
        }
        return HtmlUtils.htmlEscape(input);
    }

    /**
     * 이스케이프된 HTML을 원래 문자로 복원한다.
     * null 입력은 null을 반환한다.
     *
     * @param input 복원할 문자열
     * @return 복원된 문자열, 입력이 null이면 null
     */
    public static String unescape(String input) {
        if (input == null) {
            return null;
        }
        return HtmlUtils.htmlUnescape(input);
    }

    /**
     * 문자열에 잠재적으로 위험한 HTML 태그가 포함되어 있는지 확인한다.
     *
     * @param input 확인할 문자열
     * @return 위험한 태그가 포함되어 있으면 true
     */
    public static boolean containsHtmlTags(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        // script, iframe, object, embed, form 등 위험한 태그 패턴
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("<script") ||
                lowerInput.contains("<iframe") ||
                lowerInput.contains("<object") ||
                lowerInput.contains("<embed") ||
                lowerInput.contains("<form") ||
                lowerInput.contains("<input") ||
                lowerInput.contains("<link") ||
                lowerInput.contains("<style") ||
                lowerInput.contains("<meta") ||
                lowerInput.contains("javascript:") ||
                lowerInput.contains("vbscript:") ||
                lowerInput.contains("data:text/html") ||
                lowerInput.contains("onerror=") ||
                lowerInput.contains("onload=") ||
                lowerInput.contains("onclick=") ||
                lowerInput.contains("onmouseover=");
    }

    /**
     * 문자열에서 모든 HTML 태그를 제거한다.
     * null 입력은 null을 반환한다.
     *
     * @param input 태그를 제거할 문자열
     * @return 태그가 제거된 문자열, 입력이 null이면 null
     */
    public static String stripAllTags(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("<[^>]*>", "");
    }
}
