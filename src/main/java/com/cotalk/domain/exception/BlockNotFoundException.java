package com.cotalk.domain.exception;

/**
 * 차단 정보를 찾을 수 없을 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class BlockNotFoundException extends DomainException {

    public BlockNotFoundException(String message) {
        super(message);
    }
}
