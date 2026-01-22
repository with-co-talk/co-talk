package com.cotalk.infrastructure.config;

import com.cotalk.domain.port.inbound.auth.SignUpUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발 환경용 초기 데이터 생성기.
 * 로컬 프로파일에서만 실행되며, 테스트용 사용자 계정을 자동으로 생성한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements ApplicationRunner {

    private final SignUpUseCase signUpUseCase;
    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========================================");
        log.info("로컬 개발 환경 초기 데이터 생성 시작");
        log.info("========================================");

        createTestUserIfNotExists("a@a.com", "Nhn123!@#", "타코");
        createTestUserIfNotExists("b@b.com", "Nhn123!@#", "후추");

        log.info("========================================");
        log.info("로컬 개발 환경 초기 데이터 생성 완료");
        log.info("========================================");
    }

    private void createTestUserIfNotExists(String email, String password, String nickname) {
        if (userRepository.existsByEmail(email)) {
            log.info("사용자 이미 존재: {} ({})", nickname, email);
            return;
        }

        try {
            Long userId = signUpUseCase.signUp(email, password, nickname);
            log.info("테스트 사용자 생성 완료: {} ({}) - ID: {}", nickname, email, userId);
        } catch (Exception e) {
            log.warn("테스트 사용자 생성 실패: {} ({}) - {}", nickname, email, e.getMessage());
        }
    }
}
