package com.cotalk.adapter.outbound.persistence.auth;

import com.cotalk.domain.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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
     * 이메일과 인증 코드로 비밀번호 재설정 토큰을 조회한다.
     *
     * @param email 이메일 주소
     * @param verificationCode 6자리 인증 코드
     * @return 비밀번호 재설정 토큰 (Optional)
     */
    Optional<PasswordResetToken> findByEmailAndVerificationCode(String email, String verificationCode);

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
}
