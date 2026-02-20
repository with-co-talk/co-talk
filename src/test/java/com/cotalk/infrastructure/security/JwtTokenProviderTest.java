package com.cotalk.infrastructure.security;

import com.cotalk.infrastructure.config.properties.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET = "test-secret-key-for-testing-purposes-only-1234567890-min-256-bits";
    private static final long EXPIRATION = 3600000L; // 1시간

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(SECRET, EXPIRATION, new JwtProperties.RefreshToken(7));
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    private JwtProperties createJwtProperties(String secret, long expiration) {
        return new JwtProperties(secret, expiration, new JwtProperties.RefreshToken(7));
    }

    @Nested
    @DisplayName("generateToken 메서드")
    class GenerateToken {

        @Test
        @DisplayName("사용자 ID로 토큰을 생성할 수 있다")
        void should_generateToken_when_givenUserId() {
            // given
            Long userId = 1L;

            // when
            String token = jwtTokenProvider.generateToken(userId);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3); // JWT는 3부분으로 구성
        }

        @Test
        @DisplayName("사용자 ID와 역할로 토큰을 생성할 수 있다")
        void should_generateToken_when_givenUserIdAndRole() {
            // given
            Long userId = 1L;
            String role = "ADMIN";

            // when
            String token = jwtTokenProvider.generateToken(userId, role);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("다른 사용자 ID로 다른 토큰이 생성된다")
        void should_generateDifferentTokens_when_differentUserIds() {
            // given
            Long userId1 = 1L;
            Long userId2 = 2L;

            // when
            String token1 = jwtTokenProvider.generateToken(userId1);
            String token2 = jwtTokenProvider.generateToken(userId2);

            // then
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("기본 역할은 USER이다")
        void should_haveDefaultRoleUser_when_noRoleProvided() {
            // given
            Long userId = 1L;

            // when
            String token = jwtTokenProvider.generateToken(userId);
            String role = jwtTokenProvider.getRoleFromToken(token);

            // then
            assertThat(role).isEqualTo("USER");
        }
    }

    @Nested
    @DisplayName("getUserIdFromToken 메서드")
    class GetUserIdFromToken {

        @Test
        @DisplayName("토큰에서 사용자 ID를 추출할 수 있다")
        void should_extractUserId_when_validToken() {
            // given
            Long userId = 123L;
            String token = jwtTokenProvider.generateToken(userId);

            // when
            Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

            // then
            assertThat(extractedUserId).isEqualTo(userId);
        }

        @Test
        @DisplayName("여러 사용자의 토큰에서 각각 올바른 ID를 추출한다")
        void should_extractCorrectUserId_when_multipleUsers() {
            // given
            Long userId1 = 1L;
            Long userId2 = 999L;
            Long userId3 = Long.MAX_VALUE;

            String token1 = jwtTokenProvider.generateToken(userId1);
            String token2 = jwtTokenProvider.generateToken(userId2);
            String token3 = jwtTokenProvider.generateToken(userId3);

            // when & then
            assertThat(jwtTokenProvider.getUserIdFromToken(token1)).isEqualTo(userId1);
            assertThat(jwtTokenProvider.getUserIdFromToken(token2)).isEqualTo(userId2);
            assertThat(jwtTokenProvider.getUserIdFromToken(token3)).isEqualTo(userId3);
        }

        @Test
        @DisplayName("잘못된 토큰에서 ID 추출 시 예외 발생")
        void should_throwException_when_invalidToken() {
            // given
            String invalidToken = "invalid.token.string";

            // when & then
            assertThatThrownBy(() -> jwtTokenProvider.getUserIdFromToken(invalidToken))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("getRoleFromToken 메서드")
    class GetRoleFromToken {

        @Test
        @DisplayName("토큰에서 역할을 추출할 수 있다")
        void should_extractRole_when_validToken() {
            // given
            Long userId = 1L;
            String role = "ADMIN";
            String token = jwtTokenProvider.generateToken(userId, role);

            // when
            String extractedRole = jwtTokenProvider.getRoleFromToken(token);

            // then
            assertThat(extractedRole).isEqualTo(role);
        }

        @Test
        @DisplayName("역할이 없는 토큰에서는 기본값 USER를 반환한다")
        void should_returnDefaultUser_when_noRole() {
            // given
            Long userId = 1L;
            String token = jwtTokenProvider.generateToken(userId);

            // when
            String role = jwtTokenProvider.getRoleFromToken(token);

            // then
            assertThat(role).isEqualTo("USER");
        }

        @Test
        @DisplayName("다양한 역할을 정상적으로 추출한다")
        void should_extractDifferentRoles_correctly() {
            // given
            Long userId = 1L;
            String[] roles = {"USER", "ADMIN", "MODERATOR"};

            // when & then
            for (String role : roles) {
                String token = jwtTokenProvider.generateToken(userId, role);
                assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo(role);
            }
        }
    }

    @Nested
    @DisplayName("validateToken 메서드")
    class ValidateToken {

        @Test
        @DisplayName("유효한 토큰은 true를 반환한다")
        void should_returnTrue_when_validToken() {
            // given
            String token = jwtTokenProvider.generateToken(1L);

            // when
            boolean isValid = jwtTokenProvider.validateToken(token);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("null 토큰은 false를 반환한다")
        void should_returnFalse_when_nullToken() {
            // when
            boolean isValid = jwtTokenProvider.validateToken(null);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("빈 토큰은 false를 반환한다")
        void should_returnFalse_when_emptyToken() {
            // when
            boolean isValid = jwtTokenProvider.validateToken("");

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("잘못된 형식의 토큰은 false를 반환한다")
        void should_returnFalse_when_malformedToken() {
            // given
            String malformedToken = "not.a.valid.jwt.token";

            // when
            boolean isValid = jwtTokenProvider.validateToken(malformedToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("다른 비밀키로 서명된 토큰은 false를 반환한다")
        void should_returnFalse_when_tokenSignedWithDifferentKey() {
            // given
            String differentSecret = "different-secret-key-for-testing-purposes-only-1234567890-min";
            SecretKey differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));

            String tokenWithDifferentKey = Jwts.builder()
                    .subject("1")
                    .signWith(differentKey)
                    .compact();

            // when
            boolean isValid = jwtTokenProvider.validateToken(tokenWithDifferentKey);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("만료된 토큰은 false를 반환한다")
        void should_returnFalse_when_expiredToken() {
            // given - 이미 만료된 토큰 생성
            JwtTokenProvider shortLivedProvider = new JwtTokenProvider(createJwtProperties(SECRET, 1)); // 1ms 만료
            String token = shortLivedProvider.generateToken(1L);

            // 만료되도록 대기
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // when
            boolean isValid = jwtTokenProvider.validateToken(token);

            // then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("isTokenExpired 메서드")
    class IsTokenExpired {

        @Test
        @DisplayName("유효한 토큰은 만료되지 않음")
        void should_returnFalse_when_tokenNotExpired() {
            // given
            String token = jwtTokenProvider.generateToken(1L);

            // when
            boolean isExpired = jwtTokenProvider.isTokenExpired(token);

            // then
            assertThat(isExpired).isFalse();
        }

        @Test
        @DisplayName("만료된 토큰은 만료됨으로 판단")
        void should_returnTrue_when_tokenExpired() {
            // given - 이미 만료된 토큰 생성
            JwtTokenProvider shortLivedProvider = new JwtTokenProvider(createJwtProperties(SECRET, 1)); // 1ms 만료
            String token = shortLivedProvider.generateToken(1L);

            // 만료되도록 대기
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // when
            boolean isExpired = jwtTokenProvider.isTokenExpired(token);

            // then
            assertThat(isExpired).isTrue();
        }

        @Test
        @DisplayName("잘못된 토큰은 만료됨으로 판단")
        void should_returnTrue_when_invalidToken() {
            // given
            String invalidToken = "invalid.token";

            // when
            boolean isExpired = jwtTokenProvider.isTokenExpired(invalidToken);

            // then
            assertThat(isExpired).isTrue();
        }

        @Test
        @DisplayName("null 토큰은 만료됨으로 판단")
        void should_returnTrue_when_nullToken() {
            // when
            boolean isExpired = jwtTokenProvider.isTokenExpired(null);

            // then
            assertThat(isExpired).isTrue();
        }
    }

    @Nested
    @DisplayName("토큰 라운드 트립 테스트")
    class RoundTripTest {

        @Test
        @DisplayName("토큰 생성 후 모든 정보를 추출할 수 있다")
        void should_extractAllInfo_afterGeneratingToken() {
            // given
            Long userId = 42L;
            String role = "MODERATOR";

            // when
            String token = jwtTokenProvider.generateToken(userId, role);

            // then
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
            assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
            assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo(role);
            assertThat(jwtTokenProvider.isTokenExpired(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("토큰 타입 구분")
    class TokenType {

        @Test
        @DisplayName("Access 토큰은 token_type이 ACCESS이다")
        void should_haveAccessTokenType_when_generateToken() {
            // given
            Long userId = 1L;

            // when
            String token = jwtTokenProvider.generateToken(userId);
            String tokenType = jwtTokenProvider.getTokenType(token);

            // then
            assertThat(tokenType).isEqualTo("ACCESS");
        }

        @Test
        @DisplayName("isAccessToken은 Access 토큰에 대해 true를 반환한다")
        void should_returnTrue_when_accessToken() {
            // given
            Long userId = 1L;
            String token = jwtTokenProvider.generateToken(userId);

            // when
            boolean isAccess = jwtTokenProvider.isAccessToken(token);

            // then
            assertThat(isAccess).isTrue();
        }

        @Test
        @DisplayName("token_type이 없는 토큰은 기본적으로 ACCESS로 간주한다")
        void should_defaultToAccess_when_noTokenType() {
            // given - token_type claim이 없는 토큰 생성 (하위 호환성)
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            String tokenWithoutType = Jwts.builder()
                    .subject("1")
                    .signWith(key)
                    .compact();

            // when
            String tokenType = jwtTokenProvider.getTokenType(tokenWithoutType);

            // then
            assertThat(tokenType).isEqualTo("ACCESS");
            assertThat(jwtTokenProvider.isAccessToken(tokenWithoutType)).isTrue();
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCases {

        @Test
        @DisplayName("음수 userId로도 토큰을 생성할 수 있다")
        void should_handleNegativeUserId() {
            // given
            Long userId = -1L;

            // when
            String token = jwtTokenProvider.generateToken(userId);
            Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

            // then
            assertThat(extractedUserId).isEqualTo(userId);
        }

        @Test
        @DisplayName("0 userId로도 토큰을 생성할 수 있다")
        void should_handleZeroUserId() {
            // given
            Long userId = 0L;

            // when
            String token = jwtTokenProvider.generateToken(userId);
            Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

            // then
            assertThat(extractedUserId).isEqualTo(userId);
        }

        @Test
        @DisplayName("빈 역할로도 토큰을 생성할 수 있다")
        void should_handleEmptyRole() {
            // given
            Long userId = 1L;
            String emptyRole = "";

            // when
            String token = jwtTokenProvider.generateToken(userId, emptyRole);
            String extractedRole = jwtTokenProvider.getRoleFromToken(token);

            // then
            assertThat(extractedRole).isEqualTo(emptyRole);
        }
    }
}
