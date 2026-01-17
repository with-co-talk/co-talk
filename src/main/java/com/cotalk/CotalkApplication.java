package com.cotalk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Co-Talk 백엔드 애플리케이션 메인 클래스.
 *
 * <p>실시간 채팅 기반 커뮤니케이션 플랫폼의 백엔드 서버입니다.
 * WebSocket을 통한 실시간 메시징, JWT 기반 인증, 푸시 알림 등의 기능을 제공합니다.
 *
 * @author seunggu.lee
 * @since 1.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class CotalkApplication {

    /**
     * 애플리케이션 진입점.
     *
     * @param args 명령줄 인수
     */
    public static void main(String[] args) {
        SpringApplication.run(CotalkApplication.class, args);
    }
}
