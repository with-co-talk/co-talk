package com.cotalk.domain.port.inbound.user;

/**
 * 회원 탈퇴 유스케이스.
 * 사용자의 계정을 탈퇴 처리한다.
 *
 * @author seunggu.lee
 */
public interface DeleteAccountUseCase {

    /**
     * 회원 탈퇴를 처리한다.
     *
     * @param userId 탈퇴할 사용자 ID
     * @param password 비밀번호 확인
     */
    void deleteAccount(Long userId, String password);

    /**
     * 관리자에 의한 회원 탈퇴를 처리한다. (비밀번호 확인 없음)
     *
     * @param userId 탈퇴할 사용자 ID
     */
    void deleteAccountByAdmin(Long userId);
}
