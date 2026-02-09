package com.cotalk.domain.port.outbound;

import java.util.List;
import java.util.Map;

/**
 * 푸시 알림 전송 아웃바운드 포트.
 * 푸시 알림 전송을 위한 인터페이스를 정의한다.
 * FCM, APNs 등 다양한 푸시 서비스 구현체로 교체 가능하다.
 *
 * @author seunggu.lee
 */
public interface PushNotificationSender {

    /**
     * 단일 디바이스에 푸시 알림을 전송한다.
     *
     * @param token 디바이스 토큰
     * @param title 알림 제목
     * @param body  알림 내용
     * @param data  추가 데이터 (key-value 쌍)
     * @param imageUrl 알림에 표시할 이미지 URL (없으면 null)
     * @return 전송 성공 여부
     */
    boolean send(String token, String title, String body, Map<String, String> data, String imageUrl);

    /**
     * 여러 디바이스에 푸시 알림을 전송한다.
     *
     * @param tokens 디바이스 토큰 목록
     * @param title  알림 제목
     * @param body   알림 내용
     * @param data   추가 데이터 (key-value 쌍)
     * @param imageUrl 알림에 표시할 이미지 URL (없으면 null)
     * @return 성공한 전송 수
     */
    int sendMultiple(List<String> tokens, String title, String body, Map<String, String> data, String imageUrl);
}
