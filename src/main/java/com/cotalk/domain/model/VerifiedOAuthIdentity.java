package com.cotalk.domain.model;

/**
 * OAuth 제공자(카카오/구글/애플) 토큰을 서버에서 검증한 뒤 도출한 신뢰 가능한 사용자 식별 정보.
 *
 * <p>이 값 객체에 담긴 모든 필드는 클라이언트가 보낸 값이 아니라,
 * 제공자 토큰(카카오 access token, 구글/애플 id_token)을 서버가 직접 검증하여
 * 얻은 값이다. 따라서 인증·회원가입 로직은 클라이언트가 보낸 식별 정보 대신
 * 반드시 이 객체의 값만 신뢰해야 한다.</p>
 *
 * <p>{@code oauthId}는 항상 존재해야 하며(불변식), {@code email}/{@code nickname}/
 * {@code avatarUrl}은 제공자나 사용자 동의 범위에 따라 부재할 수 있다
 * (예: 애플은 최초 로그인 이후 이메일을 내려주지 않을 수 있다).</p>
 *
 * @param oauthId   제공자가 발급한 사용자 고유 ID (카카오 {@code id}, 구글/애플 {@code sub}). 필수.
 * @param email     검증된 이메일 (없을 수 있음)
 * @param nickname  검증된 닉네임/표시 이름 (없을 수 있음)
 * @param avatarUrl 검증된 프로필 이미지 URL (없을 수 있음)
 * @author seunggu.lee
 */
public record VerifiedOAuthIdentity(
        String oauthId,
        String email,
        String nickname,
        String avatarUrl
) {

    /**
     * 검증된 OAuth 식별 정보를 생성한다.
     *
     * @param oauthId   제공자가 발급한 사용자 고유 ID. 필수.
     * @param email     검증된 이메일 (없을 수 있음)
     * @param nickname  검증된 닉네임 (없을 수 있음)
     * @param avatarUrl 검증된 프로필 이미지 URL (없을 수 있음)
     * @throws IllegalArgumentException {@code oauthId}가 null이거나 공백인 경우
     */
    public VerifiedOAuthIdentity {
        if (oauthId == null || oauthId.isBlank()) {
            throw new IllegalArgumentException("검증된 oauthId는 비어 있을 수 없습니다.");
        }
    }
}
