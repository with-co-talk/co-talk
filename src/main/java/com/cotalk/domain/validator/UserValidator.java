package com.cotalk.domain.validator;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.SelfActionNotAllowedException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 사용자 유효성 검증기.
 * <p>
 * 사용자 관련 입력값의 유효성을 검증한다.
 * 이메일 형식, 비밀번호 강도, 닉네임 등의 유효성을 검증하는 역할을 담당한다.
 * 또한 사용자 존재 여부 및 자기 자신 액션 방지 검증을 담당한다.
 * </p>
 *
 * @author seunggu.lee
 */
@RequiredArgsConstructor
public class UserValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;

    /**
     * 이메일 형식을 검증합니다.
     *
     * @param email 검증할 이메일
     * @throws IllegalArgumentException 이메일 형식이 올바르지 않은 경우
     */
    public void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
    }

    /**
     * 비밀번호 길이를 검증합니다.
     *
     * @param password 검증할 비밀번호
     * @throws IllegalArgumentException 비밀번호가 최소 길이 미만인 경우
     */
    public void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
        }
    }

    /**
     * 닉네임이 비어있지 않은지 검증합니다.
     *
     * @param nickname 검증할 닉네임
     * @throws IllegalArgumentException 닉네임이 비어있는 경우
     */
    public void validateNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 비어있을 수 없습니다.");
        }
    }

    /**
     * 자기 자신에 대한 액션이 아닌지 검증합니다.
     *
     * @param actorId 액션을 수행하는 사용자 ID
     * @param targetId 액션 대상 사용자 ID
     * @param actionType 액션 유형 (예: "차단", "친구 요청", "신고")
     * @throws SelfActionNotAllowedException 자기 자신에 대한 액션인 경우
     */
    public void validateNotSelfAction(Long actorId, Long targetId, String actionType) {
        if (actorId.equals(targetId)) {
            throw new SelfActionNotAllowedException(actionType);
        }
    }

    /**
     * 사용자가 존재하는지 검증합니다.
     *
     * @param userId 검증할 사용자 ID
     * @return 존재하는 사용자 엔티티
     * @throws UserNotFoundException 사용자가 존재하지 않는 경우
     */
    public User validateUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * 여러 사용자가 모두 존재하는지 검증합니다.
     * 배치 조회를 통해 N+1 쿼리 문제를 방지합니다.
     *
     * @param userIds 검증할 사용자 ID 목록
     * @throws UserNotFoundException 존재하지 않는 사용자가 있는 경우
     */
    public void validateUsersExist(Iterable<Long> userIds) {
        List<Long> userIdList = StreamSupport.stream(userIds.spliterator(), false)
                .toList();

        if (userIdList.isEmpty()) {
            return;
        }

        Set<Long> existingUserIds = userRepository.findAllById(userIdList).stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        for (Long userId : userIdList) {
            if (!existingUserIds.contains(userId)) {
                throw new UserNotFoundException(userId);
            }
        }
    }
}
