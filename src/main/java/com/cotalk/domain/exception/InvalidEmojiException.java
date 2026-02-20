package com.cotalk.domain.exception;

/**
 * 유효하지 않은 이모지일 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class InvalidEmojiException extends DomainException {

    public InvalidEmojiException(String message) {
        super(message, "INVALID_EMOJI", HttpStatusHint.BAD_REQUEST);
    }

    public static InvalidEmojiException invalidFormat(String emoji) {
        return new InvalidEmojiException("유효하지 않은 이모지입니다. 지원되는 이모지만 사용할 수 있습니다: " + emoji);
    }
}
