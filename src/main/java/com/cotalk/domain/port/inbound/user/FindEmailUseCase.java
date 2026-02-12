package com.cotalk.domain.port.inbound.user;

/**
 * 아이디(이메일) 찾기 유스케이스.
 * 닉네임과 전화번호로 마스킹된 이메일을 반환한다.
 *
 * @author seunggu.lee
 */
public interface FindEmailUseCase {

    /**
     * 닉네임과 전화번호로 이메일을 찾는다.
     *
     * @param nickname 닉네임
     * @param phoneNumber 전화번호
     * @return 찾기 결과 (마스킹된 이메일 또는 실패 메시지)
     */
    FindEmailResult findEmail(String nickname, String phoneNumber);

    /**
     * 이메일 찾기 결과.
     *
     * @param found 이메일 찾기 성공 여부
     * @param maskedEmail 마스킹된 이메일 (성공 시)
     * @param message 결과 메시지
     */
    record FindEmailResult(boolean found, String maskedEmail, String message) {

        public static FindEmailResult success(String maskedEmail) {
            return new FindEmailResult(true, maskedEmail, "이메일을 찾았습니다.");
        }

        public static FindEmailResult notFound() {
            return new FindEmailResult(false, null, "일치하는 계정을 찾을 수 없습니다.");
        }
    }
}
