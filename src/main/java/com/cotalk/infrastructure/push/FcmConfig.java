package com.cotalk.infrastructure.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Cloud Messaging(FCM) 설정 클래스.
 * Firebase를 초기화하고 {@link FirebaseMessaging} 빈을 구성한다.
 *
 * <p>사용하려면 서비스 계정 키 파일이 resources 디렉토리에 필요하다.
 * 기본 경로는 {@code firebase-service-account.json}이며 설정으로 변경 가능하다.
 *
 * <p>필요한 설정 프로퍼티:
 * <ul>
 *   <li>{@code firebase.enabled} - FCM 활성화 여부 (기본값: false)</li>
 *   <li>{@code firebase.credentials-path} - 서비스 계정 키 파일 경로 (기본값: firebase-service-account.json)</li>
 * </ul>
 *
 * @author seunggu.lee
 * @see FcmPushNotificationSender
 */
@Slf4j
@Configuration
public class FcmConfig {

    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;

    @Value("${firebase.credentials-path:firebase-service-account.json}")
    private String credentialsPath;

    /**
     * Firebase 애플리케이션을 초기화한다.
     *
     * <p>Firebase가 비활성화되어 있거나 이미 초기화된 경우 건너뛴다.
     * 인증 파일이 없는 경우 경고 로그를 출력하고 초기화를 건너뛴다.
     */
    @PostConstruct
    public void initialize() {
        if (!firebaseEnabled) {
            log.info("Firebase is disabled. Push notifications will be mocked.");
            return;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                ClassPathResource resource = new ClassPathResource(credentialsPath);

                if (!resource.exists()) {
                    log.warn("Firebase credentials file not found: {}. Push notifications will be disabled.", credentialsPath);
                    return;
                }

                InputStream serviceAccount = resource.getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase", e);
        }
    }

    /**
     * FirebaseMessaging 빈을 생성한다.
     *
     * <p>Firebase가 비활성화되어 있거나 초기화되지 않은 경우 {@code null}을 반환한다.
     *
     * @return FirebaseMessaging 인스턴스 또는 {@code null}
     */
    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (!firebaseEnabled || FirebaseApp.getApps().isEmpty()) {
            return null;
        }
        return FirebaseMessaging.getInstance();
    }
}
