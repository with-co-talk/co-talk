package com.cotalk.domain.validator;

import com.cotalk.domain.exception.InvalidEmojiException;
import org.springframework.stereotype.Component;

/**
 * 메시지 관련 입력값 검증을 담당하는 Validator
 */
@Component
public class MessageValidator {

    private static final int MAX_EMOJI_LENGTH = 50;

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
     * 이모지의 유효성을 검증합니다.
     *
     * @param emoji 검증할 이모지
     * @throws InvalidEmojiException 이모지가 비어있거나 너무 긴 경우
     */
    public void validateEmoji(String emoji) {
        if (emoji == null || emoji.trim().isEmpty()) {
            throw InvalidEmojiException.invalidFormat(emoji);
        }
        if (emoji.length() > MAX_EMOJI_LENGTH) {
            throw InvalidEmojiException.tooLong(emoji);
        }
    }
}
