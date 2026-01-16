package com.cotalk.domain.port.outbound;

import java.util.List;
import java.util.Map;

/**
 * 푸시 알림 전송을 위한 아웃바운드 포트.
 * FCM, APNs 등 다양한 푸시 서비스 구현체를 교체할 수 있습니다.
 */
public interface PushNotificationSender {

    /**
     * 단일 디바이스에 푸시 알림 전송
     * @param token 디바이스 토큰
     * @param title 알림 제목
     * @param body 알림 내용
     * @param data 추가 데이터
     * @return 전송 성공 여부
     */
    boolean send(String token, String title, String body, Map<String, String> data);

    /**
     * 여러 디바이스에 푸시 알림 전송
     * @param tokens 디바이스 토큰 목록
     * @param title 알림 제목
     * @param body 알림 내용
     * @param data 추가 데이터
     * @return 성공한 전송 수
     */
    int sendMultiple(List<String> tokens, String title, String body, Map<String, String> data);
}
