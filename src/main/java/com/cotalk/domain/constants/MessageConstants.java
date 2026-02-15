package com.cotalk.domain.constants;

/**
 * 메시지 관련 상수를 정의하는 클래스.
 * 매직 넘버를 상수로 관리하여 유지보수성을 향상시킨다.
 *
 * @author seunggu.lee
 */
public final class MessageConstants {

    private MessageConstants() {
        // 인스턴스 생성 방지
    }

    /** 메시지 최대 길이 (자) */
    public static final int MAX_MESSAGE_LENGTH = 5000;

    /** 푸시 알림 미리보기 최대 길이 (자) */
    public static final int MAX_NOTIFICATION_PREVIEW_LENGTH = 100;

    /** 메시지 수정/삭제 가능 시간 (분) */
    public static final int EDIT_TIME_LIMIT_MINUTES = 5;
}
