package com.cotalk.integration;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.port.inbound.message.SearchMessageUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.inbound.message.UpdateMessageUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.config.TestRedisConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 암호화 ON + 실제 Flyway(PostgreSQL testcontainer) 환경에서 블라인드 인덱스 메시지 검색이
 * 실제로 동작함을 검증하는 통합 테스트 (재발방지 핵심 산출물).
 *
 * <p>기존 검색 실패의 근본 원인은 단위/JPA 테스트가 {@code app.encryption.enabled=false}로 돌아
 * 평문 LIKE가 통과했고, prod(암호화 ON)에서만 깨진 것이다. 이 테스트는 prod 동등 환경
 * (PostgreSQL + 실제 Flyway V17 + 암호화 ON + 고정 키/시크릿)에서 검색을 검증해 그 괴리를 막는다.</p>
 *
 * <p>검증 시나리오: 한국어 메시지 저장 시 DB raw content가 평문과 다름(=암호화 확인) →
 * 부분일치("밀번") 검색 매칭 → 1~2글자 거부 → false positive(트라이그램은 맞지만 substring 아님)의
 * 2단계 제거 → 소프트삭제 제외 → 멤버십 격리 → over-fetch 페이징 → 수정 재토큰화.</p>
 *
 * @author seunggu.lee
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@DisplayName("암호화 ON 블라인드 인덱스 검색 통합")
class MessageSearchBlindIndexIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cotalk")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        // PostgreSQL + 실제 Flyway 마이그레이션(V17 포함)으로 prod 동등 스키마 구성
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        // ddl-auto는 끄고 Flyway가 스키마를 소유 (V17 message_search_tokens 실제 적용)
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        // 핵심: 암호화 ON + 고정 키/시크릿 (prod 동등)
        registry.add("app.encryption.enabled", () -> "true");
        registry.add("app.encryption.key", () -> "dGhpc2lzYXRlc3RrZXlmb3JkZXZlbG9wbWVudG9ubHk=");
        registry.add("app.search.blind-index-secret",
                () -> "dGVzdC1ibGluZC1pbmRleC1zZWNyZXQtZm9yLWludGVncmF0aW9uLXRlc3Q=");
    }

    @Autowired
    private SendMessageUseCase sendMessageUseCase;
    @Autowired
    private SearchMessageUseCase searchMessageUseCase;
    @Autowired
    private UpdateMessageUseCase updateMessageUseCase;
    @Autowired
    private MessageRepository messageRepository;
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

    @AfterEach
    void cleanUp() {
        // 물리 삭제로 테스트 간 격리 (토큰은 FK CASCADE로 함께 삭제됨)
        jdbcTemplate.update("DELETE FROM message_search_tokens");
        jdbcTemplate.update("DELETE FROM messages");
        jdbcTemplate.update("DELETE FROM chat_room_members");
        jdbcTemplate.update("DELETE FROM chat_rooms");
        jdbcTemplate.update("DELETE FROM users");
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

    @Test
    @DisplayName("한국어 메시지는 DB에 암호문으로 저장되고(평문과 다름) 부분일치 검색이 매칭한다")
    void should_encryptContent_andMatchPartialKeyword() {
        Long sender = createUser("a@test.com", "철수");
        Long roomId = createRoomWithMembers(sender);

        Message sent = sendMessageUseCase.sendMessage(roomId, sender, "오늘 비밀번호를 변경했어요");

        // DB raw content가 평문과 다름 = 실제 암호화 확인 (이게 핵심 — 항진이 아님을 보장)
        String rawContent = jdbcTemplate.queryForObject(
                "SELECT content FROM messages WHERE id = ?", String.class, sent.getId());
        assertThat(rawContent).isNotNull().isNotEqualTo("오늘 비밀번호를 변경했어요");
        assertThat(rawContent).doesNotContain("비밀번호");
        // 토큰이 실제 적재됐는지 확인
        Integer tokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM message_search_tokens WHERE message_id = ?", Integer.class, sent.getId());
        assertThat(tokenCount).isGreaterThan(0);

        // 부분일치 검색 ("밀번" — 단어 중간 부분 문자열)
        List<Message> results = searchMessageUseCase.searchInChatRoom(roomId, sender, "밀번호", 0, 20);
        assertThat(results).extracting(Message::getId).containsExactly(sent.getId());
        assertThat(results.get(0).getContent()).isEqualTo("오늘 비밀번호를 변경했어요"); // 복호화 확인
    }

    @Test
    @DisplayName("1~2글자 키워드는 검색을 거부한다")
    void should_rejectKeyword_when_shorterThanThree() {
        Long sender = createUser("b@test.com", "영희");
        Long roomId = createRoomWithMembers(sender);
        sendMessageUseCase.sendMessage(roomId, sender, "안녕하세요 반갑습니다");

        assertThatThrownBy(() -> searchMessageUseCase.searchInChatRoom(roomId, sender, "안녕", 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3글자");
    }

    @Test
    @DisplayName("트라이그램은 맞지만 substring이 아닌 false positive는 2단계 복호화 검증에서 제거된다")
    void should_removeFalsePositive_when_substringNotPresent() {
        Long sender = createUser("c@test.com", "민수");
        Long roomId = createRoomWithMembers(sender);

        // "비밀번호"의 트라이그램(비밀번/밀번호)이 흩어져 존재하지만 연속 substring은 아닌 메시지
        Message falsePositive = sendMessageUseCase.sendMessage(roomId, sender, "비밀번 얘기와 번호 변경");
        Message realHit = sendMessageUseCase.sendMessage(roomId, sender, "새 비밀번호 설정 완료");

        List<Message> results = searchMessageUseCase.searchInChatRoom(roomId, sender, "비밀번호", 0, 20);

        // 1단계 토큰 AND는 둘 다 후보가 될 수 있으나, 2단계 substring으로 realHit만 남아야 함
        assertThat(results).extracting(Message::getId).containsExactly(realHit.getId());
        assertThat(results).extracting(Message::getId).doesNotContain(falsePositive.getId());
    }

    @Test
    @DisplayName("소프트 삭제된 메시지는 검색에서 제외된다")
    void should_excludeSoftDeletedMessages() {
        Long sender = createUser("d@test.com", "지수");
        Long roomId = createRoomWithMembers(sender);

        Message alive = sendMessageUseCase.sendMessage(roomId, sender, "회의시간 정합시다");
        Message toDelete = sendMessageUseCase.sendMessage(roomId, sender, "회의시간 취소됐어요");

        // 소프트 삭제 (토큰은 유지되지만 검색에서 deleted 필터로 제외돼야 함)
        toDelete.delete(java.time.LocalDateTime.now());
        messageRepository.save(toDelete);

        List<Message> results = searchMessageUseCase.searchInChatRoom(roomId, sender, "회의시간", 0, 20);
        assertThat(results).extracting(Message::getId).containsExactly(alive.getId());
    }

    @Test
    @DisplayName("멤버가 아닌 방의 메시지는 검색되지 않는다 (멤버십 격리)")
    void should_isolateByMembership() {
        Long alice = createUser("e@test.com", "앨리스");
        Long bob = createUser("f@test.com", "밥");
        Long aliceRoom = createRoomWithMembers(alice);
        Long bobRoom = createRoomWithMembers(bob);

        sendMessageUseCase.sendMessage(aliceRoom, alice, "공통키워드 앨리스방");
        sendMessageUseCase.sendMessage(bobRoom, bob, "공통키워드 밥방");

        // 전체 검색: alice는 자기 방 메시지만 봐야 함
        List<Message> aliceResults = searchMessageUseCase.searchAcrossAllChatRooms(alice, "공통키워드", 0, 20);
        assertThat(aliceResults).hasSize(1);
        assertThat(aliceResults.get(0).getChatRoomId()).isEqualTo(aliceRoom);

        // 채팅방 검색: alice가 bob의 방을 검색하면 멤버십 거부
        assertThatThrownBy(() -> searchMessageUseCase.searchInChatRoom(bobRoom, alice, "공통키워드", 0, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("over-fetch 페이징: substring 통과분을 size만큼만 반환한다")
    void should_limitToSize_afterOverFetch() {
        Long sender = createUser("g@test.com", "토끼");
        Long roomId = createRoomWithMembers(sender);

        for (int i = 0; i < 5; i++) {
            sendMessageUseCase.sendMessage(roomId, sender, "회의시간 안내 " + i);
        }

        List<Message> results = searchMessageUseCase.searchInChatRoom(roomId, sender, "회의시간", 0, 2);
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("메시지 수정 시 재토큰화되어 새 키워드로 검색되고 옛 키워드로는 검색되지 않는다")
    void should_reTokenize_onUpdate() {
        Long sender = createUser("h@test.com", "거북이");
        Long roomId = createRoomWithMembers(sender);

        Message msg = sendMessageUseCase.sendMessage(roomId, sender, "원본키워드 메시지");
        // 옛 키워드로 검색되는지 먼저 확인
        assertThat(searchMessageUseCase.searchInChatRoom(roomId, sender, "원본키워드", 0, 20)).hasSize(1);

        // 수정 (5분 이내라 가능) — 본문 교체
        updateMessageUseCase.updateMessage(msg.getId(), sender, "변경키워드 새내용");

        // 새 키워드로는 검색되고
        assertThat(searchMessageUseCase.searchInChatRoom(roomId, sender, "변경키워드", 0, 20))
                .extracting(Message::getId).containsExactly(msg.getId());
        // 옛 키워드로는 검색되지 않아야 함 (delete-then-insert 재토큰화)
        assertThat(searchMessageUseCase.searchInChatRoom(roomId, sender, "원본키워드", 0, 20)).isEmpty();
    }
}
