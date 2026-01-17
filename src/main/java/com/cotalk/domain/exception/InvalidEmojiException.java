package com.cotalk.domain.exception;

/**
 * 유효하지 않은 이모지일 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class InvalidEmojiException extends DomainException {

    public InvalidEmojiException(String message) {
        super(message);
    }

    public static InvalidEmojiException invalidFormat(String emoji) {
        return new InvalidEmojiException("유효하지 않은 이모지 형식입니다: " + emoji);
    }

    public static InvalidEmojiException tooLong(String emoji) {
        return new InvalidEmojiException("이모지는 50자 이하여야 합니다: " + emoji);
    }
}
