package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.PasswordMismatchException;
import com.cotalk.domain.exception.RateLimitExceededException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.auth.ChangePasswordUseCase;
import com.cotalk.domain.port.outbound.PasswordEncoderPort;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.util.LogMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 비밀번호 변경 유스케이스 구현체.
 * 현재 비밀번호를 확인 후 새 비밀번호로 변경한다.
 * 5회 연속 실패 시 10분간 잠금한다. 실패 기록은 Redis에 저장된다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChangePasswordService implements ChangePasswordUseCase {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()\\-_=+])[A-Za-z\\d@$!%*?&#^()\\-_=+]{8,128}$"
    );
    private static final int MAX_FAILURES = 5;
    private static final long LOCKOUT_DURATION_SECONDS = 600; // 10 minutes

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 비밀번호를 변경한다.
     *
     * @param userId 사용자 ID
     * @param currentPassword 현재 비밀번호
     * @param newPassword 새 비밀번호
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     * @throws RateLimitExceededException 5회 연속 실패로 잠금된 경우
     * @throws PasswordMismatchException 현재 비밀번호가 일치하지 않는 경우
     * @throws IllegalArgumentException 새 비밀번호가 보안 요구사항을 충족하지 않는 경우
     */
    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        checkRateLimit(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            recordFailure(userId);
            throw new PasswordMismatchException("현재 비밀번호가 일치하지 않습니다.");
        }

        validatePasswordStrength(newPassword);

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.updatePassword(encodedPassword);
        userRepository.save(user);

        // 성공 시 실패 기록 초기화
        redisTemplate.delete("password:fail:" + userId);

        log.info("Password changed for user: {}", LogMaskingUtil.maskEmail(user.getEmail().value()));
    }

    private void checkRateLimit(Long userId) {
        String key = "password:fail:" + userId;
        String countStr = redisTemplate.opsForValue().get(key);
        if (countStr != null) {
            int count = Integer.parseInt(countStr);
            if (count >= MAX_FAILURES) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                long remainingSeconds = (ttl != null && ttl > 0) ? ttl : LOCKOUT_DURATION_SECONDS;
                throw RateLimitExceededException.tooManyRequests(remainingSeconds);
            }
        }
    }

    private void recordFailure(Long userId) {
        String key = "password:fail:" + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 첫 실패 시 TTL 설정
            redisTemplate.expire(key, LOCKOUT_DURATION_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void validatePasswordStrength(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "비밀번호는 8-128자이며, 대문자, 소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.");
        }
    }
}
