package com.cotalk.application.service.user;

import com.cotalk.domain.port.inbound.user.FindEmailUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아이디(이메일) 찾기 유스케이스 구현체.
 * 닉네임과 전화번호로 사용자를 조회하고 마스킹된 이메일을 반환한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindEmailService implements FindEmailUseCase {

    private final UserRepository userRepository;

    @Override
    public FindEmailResult findEmail(String nickname, String phoneNumber) {
        return userRepository.findByNicknameAndPhoneNumber(nickname, phoneNumber)
                .map(user -> FindEmailResult.success(maskEmail(user.getEmail().value())))
                .orElse(FindEmailResult.notFound());
    }

    /**
     * 이메일을 마스킹한다.
     * 예: "test@example.com" -> "te***@exam***.com"
     *
     * @param email 원본 이메일
     * @return 마스킹된 이메일
     */
    String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];

        // 로컬 파트 마스킹: 처음 2글자만 표시
        String maskedLocal;
        if (localPart.length() <= 2) {
            maskedLocal = localPart.charAt(0) + "***";
        } else {
            maskedLocal = localPart.substring(0, 2) + "***";
        }

        // 도메인 파트 마스킹: 처음 4글자만 표시
        String maskedDomain;
        int dotIndex = domainPart.lastIndexOf('.');
        if (dotIndex > 0) {
            String domainName = domainPart.substring(0, dotIndex);
            String tld = domainPart.substring(dotIndex);
            if (domainName.length() <= 4) {
                maskedDomain = domainName.charAt(0) + "***" + tld;
            } else {
                maskedDomain = domainName.substring(0, 4) + "***" + tld;
            }
        } else {
            maskedDomain = domainPart.length() > 2
                    ? domainPart.substring(0, 2) + "***"
                    : domainPart;
        }

        return maskedLocal + "@" + maskedDomain;
    }
}
