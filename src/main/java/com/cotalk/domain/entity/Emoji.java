package com.cotalk.domain.entity;

import java.util.Optional;

/**
 * 메시지 반응에 사용 가능한 이모지 열거형.
 * <p>
 * 채팅 메시지에 반응할 수 있는 이모지 목록을 정의한다.
 * 각 이모지는 실제 이모지 문자와 이름을 포함한다.
 *
 * @author seunggu.lee
 */
public enum Emoji {
    /** 👍 엄지척 */
    THUMBS_UP("👍", "thumbsup"),
    /** 👎 엄지 내리기 */
    THUMBS_DOWN("👎", "thumbsdown"),
    /** ❤️ 하트 */
    HEART("❤️", "heart"),
    /** 😂 웃음 */
    LAUGHING("😂", "laughing"),
    /** 😮 놀람 */
    SURPRISED("😮", "surprised"),
    /** 😢 슬픔 */
    SAD("😢", "sad"),
    /** 🔥 불 */
    FIRE("🔥", "fire"),
    /** 🎉 파티 */
    PARTY("🎉", "party"),
    /** 👏 박수 */
    CLAPPING("👏", "clapping"),
    /** ✅ 체크 */
    CHECK("✅", "check");

    private final String character;
    private final String name;

    Emoji(String character, String name) {
        this.character = character;
        this.name = name;
    }

    /**
     * 이모지 문자를 반환한다.
     *
     * @return 이모지 문자
     */
    public String getCharacter() {
        return character;
    }

    /**
     * 이모지 이름을 반환한다.
     *
     * @return 이모지 이름
     */
    public String getName() {
        return name;
    }

    /**
     * 문자열(이모지 문자 또는 이름)로부터 Emoji를 찾는다.
     *
     * @param value 이모지 문자 또는 이름
     * @return 찾은 Emoji를 담은 Optional, 없으면 빈 Optional
     */
    public static Optional<Emoji> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        for (Emoji emoji : values()) {
            if (emoji.character.equals(value) || emoji.name.equals(value)) {
                return Optional.of(emoji);
            }
        }
        return Optional.empty();
    }

    /**
     * 문자열이 유효한 이모지인지 확인한다.
     *
     * @param value 확인할 문자열
     * @return 유효한 이모지이면 true
     */
    public static boolean isValid(String value) {
        return fromString(value).isPresent();
    }
}
