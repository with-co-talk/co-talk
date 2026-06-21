package com.cotalk.integration;

import com.cotalk.config.TestRedisConfiguration;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.config.properties.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <b>프로덕션 SecurityFilterChain 통합 테스트 — 인증/인가 경계의 재발방지 핵심 산출물.</b>
 *
 * <p>기존 통합 테스트는 {@code IntegrationTestSecurityConfig}(모든 요청 permitAll + 요청 파라미터로
 * userId 주입)로 프로덕션 필터 체인을 <b>대체</b>하거나 {@code addFilters=false}로 보안 필터를 끄고
 * 돌았다. 그 결과 JWT 인증 경계와 IDOR 보호가 어떤 테스트로도 증명되지 않았고, 이것이 OAuth
 * 계정탈취(C-1)가 숨을 수 있었던 근본 구조였다.</p>
 *
 * <p>이 테스트는 그 괴리를 닫는다:
 * <ul>
 *   <li>{@code IntegrationTestSecurityConfig}를 <b>import 하지 않는다</b> →
 *       {@code application-test.yml}에서 {@code app.security.default-chain.enabled}가 미설정이므로
 *       (matchIfMissing=true) 프로덕션 {@link com.cotalk.infrastructure.security.SecurityConfig}의
 *       실제 {@code securityFilterChain}이 활성화된다.</li>
 *   <li>{@code addFilters=false}를 <b>쓰지 않는다</b> → 실제
 *       {@link com.cotalk.infrastructure.security.JwtAuthenticationFilter}가 모든 요청을 검사한다.</li>
 *   <li>PostgreSQL testcontainer + 실제 Flyway + 암호화 ON({@link PostgresIntegrationTestBase})로
 *       prod 동등 환경에서 검증한다.</li>
 * </ul>
 *
 * <p>JWT는 앱의 실제 서명 설정({@link JwtProperties#secret()})으로 만든다. 위조 토큰만 잘못된 키로
 * 서명해 서명 검증 실패를 유도한다.</p>
 *
 * @author seunggu.lee
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestRedisConfiguration.class)
@DisplayName("프로덕션 보안 필터 체인 통합 (JWT 인증/IDOR)")
class SecurityFilterChainIntegrationTest extends PostgresIntegrationTestBase {

    private static final String TOKEN_TYPE_CLAIM = "token_type";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private SendMessageUseCase sendMessageUseCase;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;
    @Autowired
    private IdGenerator idGenerator;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userAId;
    private Long userBId;
    private Long roomAId;
    private Long messageAId;

    @BeforeEach
    void setUp() {
        userAId = createUser("alice@test.com", "앨리스");
        userBId = createUser("bob@test.com", "밥");
        // userA만 멤버인 방 + userA의 메시지 (IDOR 대상)
        roomAId = createRoomWithMembers(userAId);
        Message sent = sendMessageUseCase.sendMessage(roomAId, userAId, "앨리스의 비밀 메시지");
        messageAId = sent.getId();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM message_search_tokens");
        jdbcTemplate.update("DELETE FROM messages");
        jdbcTemplate.update("DELETE FROM chat_room_members");
        jdbcTemplate.update("DELETE FROM chat_rooms");
        jdbcTemplate.update("DELETE FROM users");
    }

    // ---------------------------------------------------------------------
    // 인증 경계: 401
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Authorization 헤더가 없으면 보호된 엔드포인트는 401을 반환한다")
    void should_return401_when_noAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/chat/messages/rooms/{roomId}", roomAId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료된 액세스 토큰은 401을 반환한다")
    void should_return401_when_accessTokenExpired() throws Exception {
        String expiredToken = buildToken(userAId, "ACCESS", signingKey(), -3600_000L);

        mockMvc.perform(get("/api/v1/chat/messages/rooms/{roomId}", roomAId)
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("위조 서명(다른 키로 서명한) JWT는 401을 반환한다")
    void should_return401_when_signatureForged() throws Exception {
        // 프로덕션 시크릿과 다른 키로 서명 → 서명 검증 실패
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "completely-different-wrong-signing-key-1234567890".getBytes(StandardCharsets.UTF_8));
        String forgedToken = buildToken(userAId, "ACCESS", wrongKey, 3600_000L);

        mockMvc.perform(get("/api/v1/chat/messages/rooms/{roomId}", roomAId)
                        .header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("액세스 토큰이 아닌 토큰(리프레시 token_type)을 bearer로 쓰면 거부되어 401을 반환한다")
    void should_return401_when_nonAccessTokenUsedAsBearer() throws Exception {
        // 올바른 키로 서명했으나 token_type=REFRESH → JwtAuthenticationFilter의 isAccessToken에서 거부
        String refreshToken = buildToken(userAId, "REFRESH", signingKey(), 3600_000L);

        mockMvc.perform(get("/api/v1/chat/messages/rooms/{roomId}", roomAId)
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------------
    // 정상 경로: 200
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("유효한 액세스 토큰으로 본인 방에 접근하면 200을 반환한다")
    void should_return200_when_validAccessTokenOnOwnResource() throws Exception {
        String validToken = buildToken(userAId, "ACCESS", signingKey(), 3600_000L);

        mockMvc.perform(get("/api/v1/chat/messages/rooms/{roomId}", roomAId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------------
    // IDOR: 인증은 됐으나 타인 리소스 접근은 거부 (403/404)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("IDOR: userB의 유효한 토큰으로 userA 전용 방의 메시지를 조회하면 거부된다 (403/404)")
    void should_denyAccess_when_userBReadsUserAOnlyRoom() throws Exception {
        String userBToken = buildToken(userBId, "ACCESS", signingKey(), 3600_000L);

        int statusCode = mockMvc.perform(get("/api/v1/chat/messages/rooms/{roomId}", roomAId)
                        .header("Authorization", "Bearer " + userBToken))
                .andReturn().getResponse().getStatus();

        // 인증은 통과(401 아님)했으나 멤버십 격리로 거부되어야 한다. 앱 계약상 403 또는 404.
        org.assertj.core.api.Assertions.assertThat(statusCode)
                .as("userB가 userA 전용 방을 조회하면 인가 거부(403/404)되어야 함")
                .isIn(403, 404);
    }

    @Test
    @DisplayName("IDOR: userB의 유효한 토큰으로 userA의 메시지를 수정하면 403을 반환한다")
    void should_return403_when_userBUpdatesUserAMessage() throws Exception {
        String userBToken = buildToken(userBId, "ACCESS", signingKey(), 3600_000L);

        mockMvc.perform(put("/api/v1/chat/messages/{messageId}", messageAId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"해킹 시도\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 지정 만료 오프셋(ms)으로 JWT를 만든다. 음수 오프셋이면 만료된 토큰이 된다.
     *
     * @param userId      subject
     * @param tokenType   token_type 클레임 (ACCESS / REFRESH)
     * @param key         서명 키 (위조 시 잘못된 키 주입)
     * @param expiryOffMs now 기준 만료 오프셋 (ms)
     * @return 컴팩트 직렬화된 JWT
     */
    private String buildToken(Long userId, String tokenType, SecretKey key, long expiryOffMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", "USER")
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(new Date(now.getTime() - 10_000L))
                .expiration(new Date(now.getTime() + expiryOffMs))
                .signWith(key)
                .compact();
    }

    private Long createUser(String email, String nickname) {
        Long id = idGenerator.nextId();
        userRepository.save(User.builder()
                .id(id)
                .email(new Email(email))
                .nickname(nickname)
                .passwordHash("$2a$10$dummyhashdummyhashdummyhashdummyhashdummyhash")
                .build());
        return id;
    }

    private Long createRoomWithMembers(Long... userIds) {
        Long roomId = idGenerator.nextId();
        chatRoomRepository.save(ChatRoom.builder().id(roomId).type(ChatRoom.ChatRoomType.GROUP).name("방").build());
        for (Long userId : userIds) {
            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(idGenerator.nextId()).chatRoomId(roomId).userId(userId).build());
        }
        return roomId;
    }
}
