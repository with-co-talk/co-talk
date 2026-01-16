package com.cotalk.domain.port.inbound;

/**
 * 회원 탈퇴 유즈케이스
 */
public interface DeleteAccountUseCase {

    /**
     * 회원 탈퇴 처리
     * 
     * @param userId 탈퇴할 사용자 ID
     * @param password 비밀번호 확인
     */
    void deleteAccount(Long userId, String password);

    /**
     * 회원 탈퇴 처리 (비밀번호 확인 없이 - 관리자용)
     *
     * @param userId 탈퇴할 사용자 ID
     */
    void deleteAccountByAdmin(Long userId);
}
