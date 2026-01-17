package com.cotalk.domain.validator;

import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.exception.InvalidEmojiException;
import org.springframework.stereotype.Component;

/**
 * 메시지 유효성 검증기.
 * <p>
 * 메시지 관련 입력값의 유효성을 검증한다.
 * 메시지 내용, 이모지 등의 형식과 길이를 검증하는 역할을 담당한다.
 * </p>
 *
 * @author seunggu.lee
 */
@Component
public class MessageValidator {

    /**
     * 메시지 내용이 비어있지 않은지 검증합니다.
     *
     * @param content 검증할 메시지 내용
     * @throws IllegalArgumentException 메시지 내용이 비어있는 경우
     */
    public void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용은 비어있을 수 없습니다.");
        }
    }

    /**
     * 이모지 문자열의 유효성을 검증하고 Emoji enum으로 변환합니다.
     *
     * @param emojiString 검증할 이모지 문자열 (이모지 문자 또는 이름)
     * @return 유효한 Emoji enum
     * @throws InvalidEmojiException 이모지가 유효하지 않은 경우
     */
    public Emoji validateAndParseEmoji(String emojiString) {
        if (emojiString == null || emojiString.trim().isEmpty()) {
            throw InvalidEmojiException.invalidFormat(emojiString);
        }

        return Emoji.fromString(emojiString.trim())
                .orElseThrow(() -> InvalidEmojiException.invalidFormat(emojiString));
    }
}
