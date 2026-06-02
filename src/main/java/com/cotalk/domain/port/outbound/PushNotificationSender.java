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

    /**
     * 디바이스별로 서로 다른 배지 값을 적용하여 푸시 알림을 전송한다.
     *
     * <p>각 대상({@link PushTarget})은 자신의 토큰과 배지 값을 가진다.
     * iOS는 APNs 배지로, Android는 알림 개수(notification count)로 best-effort 적용된다.
     * 배지 값이 {@code null}인 대상은 배지를 변경하지 않는다(기존 배지 유지).</p>
     *
     * @param targets  전송 대상 목록 (각 대상의 토큰과 배지 값)
     * @param title    알림 제목
     * @param body     알림 내용
     * @param data     추가 데이터 (key-value 쌍)
     * @param imageUrl 알림에 표시할 이미지 URL (없으면 null)
     * @return 성공한 전송 수
     */
    int sendEachWithBadge(List<PushTarget> targets, String title, String body, Map<String, String> data, String imageUrl);

    /**
     * 배지 값을 포함한 푸시 전송 대상.
     *
     * @param token 디바이스 토큰
     * @param badge 적용할 배지 값 ({@code null}이면 배지를 변경하지 않음)
     */
    record PushTarget(String token, Integer badge) {
    }
}
