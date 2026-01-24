package com.cotalk.infrastructure.config;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.auth.SignUpUseCase;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
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
    private final FriendRepository friendRepository;
    private final IdGenerator idGenerator;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========================================");
        log.info("로컬 개발 환경 초기 데이터 생성 시작");
        log.info("========================================");

        Long tacoUserId = createTestUserIfNotExists("a@a.com", "Nhn123!@#", "타코");
        Long georgiaUserId = createTestUserIfNotExists("b@b.com", "Nhn123!@#", "조지아");

        // 타코와 조지아를 친구 관계로 설정
        if (tacoUserId != null && georgiaUserId != null) {
            createFriendRelationshipIfNotExists(tacoUserId, georgiaUserId, "타코", "조지아");
        }

        log.info("========================================");
        log.info("로컬 개발 환경 초기 데이터 생성 완료");
        log.info("========================================");
    }

    private Long createTestUserIfNotExists(String email, String password, String nickname) {
        if (userRepository.existsByEmail(email)) {
            log.info("사용자 이미 존재: {} ({})", nickname, email);
            return userRepository.findByEmail(email)
                    .map(User::getId)
                    .orElse(null);
        }

        try {
            Long userId = signUpUseCase.signUp(email, password, nickname);
            log.info("테스트 사용자 생성 완료: {} ({}) - ID: {}", nickname, email, userId);
            return userId;
        } catch (Exception e) {
            log.warn("테스트 사용자 생성 실패: {} ({}) - {}", nickname, email, e.getMessage());
            return null;
        }
    }

    /**
     * 두 사용자를 친구 관계로 설정한다.
     * 양방향 친구 관계를 생성한다.
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @param nickname1 첫 번째 사용자 닉네임
     * @param nickname2 두 번째 사용자 닉네임
     */
    private void createFriendRelationshipIfNotExists(Long userId1, Long userId2, String nickname1, String nickname2) {
        // 이미 친구 관계가 있는지 확인
        if (friendRepository.existsByUserIdAndFriendId(userId1, userId2)) {
            log.info("친구 관계 이미 존재: {} <-> {}", nickname1, nickname2);
            return;
        }

        try {
            // 양방향 친구 관계 생성
            Friend friend1 = Friend.builder()
                    .id(idGenerator.nextId())
                    .userId(userId1)
                    .friendId(userId2)
                    .status(Friend.FriendStatus.ACCEPTED)
                    .build();

            Friend friend2 = Friend.builder()
                    .id(idGenerator.nextId())
                    .userId(userId2)
                    .friendId(userId1)
                    .status(Friend.FriendStatus.ACCEPTED)
                    .build();

            friendRepository.save(friend1);
            friendRepository.save(friend2);

            log.info("친구 관계 생성 완료: {} <-> {}", nickname1, nickname2);
        } catch (Exception e) {
            log.warn("친구 관계 생성 실패: {} <-> {} - {}", nickname1, nickname2, e.getMessage());
        }
    }
}
