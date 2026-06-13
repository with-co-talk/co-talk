package com.cotalk.adapter.outbound.persistence.auth;

import com.cotalk.domain.entity.PasswordResetToken;
import com.cotalk.domain.model.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 비밀번호 재설정 토큰 JPA 리포지토리.
 * Spring Data JPA를 통해 비밀번호 재설정 토큰 데이터에 접근한다.
 *
 * @author seunggu.lee
 */
public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * 토큰 값으로 비밀번호 재설정 토큰을 조회한다.
     *
     * @param token 토큰 값
     * @return 비밀번호 재설정 토큰 (Optional)
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * 이메일로 가장 최근에 발급된 미사용 인증 코드 토큰을 조회한다.
     * <p>
     * 인증 코드를 조회 조건에서 제외하여 코드 불일치 시에도 토큰을 찾아
     * 실패 횟수를 집계할 수 있도록 한다. 사용 완료(used)된 토큰은 제외한다.
     * </p>
     *
     * @param email 이메일 주소
     * @return 비밀번호 재설정 토큰 (Optional)
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.email = :email AND t.verificationCode IS NOT NULL "
            + "AND t.usedAt IS NULL ORDER BY t.id DESC LIMIT 1")
    Optional<PasswordResetToken> findLatestActiveByEmail(@Param("email") Email email);

    /**
     * 사용자 ID로 비밀번호 재설정 토큰을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 만료된 토큰들을 일괄 삭제한다.
     *
     * @param now 현재 시각
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);

    /**
     * 인증 코드 실패 횟수를 원자적으로 1 증가시킨다.
     * <p>
     * 동시 오답 요청에서 read-modify-write 경합으로 인한 lost-update를 방지하기 위해
     * DB 레벨의 원자적 조건부 UPDATE({@code SET failed_attempts = failed_attempts + 1})를 사용한다.
     * 기존 {@code ChatRoomMember} 읽음 처리 등에서 쓰는 원자적 UPDATE 패턴과 동일하다.
     * </p>
     *
     * @param id 대상 토큰 ID
     * @return 갱신된 행 수(정상 시 1)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PasswordResetToken t SET t.failedAttempts = t.failedAttempts + 1 WHERE t.id = :id")
    int incrementFailedAttempts(@Param("id") Long id);

    /**
     * 토큰의 현재 실패 횟수를 조회한다.
     * 원자적 증가({@link #incrementFailedAttempts(Long)}) 직후 최신 값을 다시 읽기 위해 사용한다.
     *
     * @param id 대상 토큰 ID
     * @return 현재 실패 횟수 (토큰이 없으면 Optional.empty)
     */
    @Query("SELECT t.failedAttempts FROM PasswordResetToken t WHERE t.id = :id")
    Optional<Integer> findFailedAttemptsById(@Param("id") Long id);
}
