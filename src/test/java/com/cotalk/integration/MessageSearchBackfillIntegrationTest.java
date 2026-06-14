package com.cotalk.integration;

import com.cotalk.application.service.message.MessageSearchBackfillService;
import com.cotalk.application.service.message.MessageSearchBackfillService.BackfillOptions;
import com.cotalk.application.service.message.MessageSearchBackfillService.BackfillResult;
import com.cotalk.config.TestRedisConfiguration;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.port.inbound.message.SearchMessageUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
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

/**
 * 기존 암호화 메시지 검색 토큰 백필(PR2) 통합 테스트.
 *
 * <p>암호화 ON + 실제 Flyway(PostgreSQL testcontainer)에서, 토큰 없이 저장된 "기존" 메시지를
 * 백필이 복호화→토큰화→적재하여 과거 메시지가 다시 검색됨을 검증한다(재발방지의 PR2 산출물).</p>
 *
 * <p>레거시 상태 재현: PR1 발송 경로가 자동 적재한 토큰을 {@code message_search_tokens}에서
 * 비워(=PR1 이전에 저장된 메시지 상태) 검색이 0건이 되게 한 뒤 백필을 실행한다.</p>
 *
 * <p>검증 시나리오: (1) 백필 전 검색 0건 → 백필 후 과거 메시지 검색됨, (2) 재실행해도 중복/오류 없음
 * (idempotent), (3) TEXT만 처리(FILE/SYSTEM 제외), (4) 소프트 삭제 메시지는 검색에서 제외.</p>
 *
 * @author seunggu.lee
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@DisplayName("기존 암호화 메시지 백필 통합")
class MessageSearchBackfillIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cotalk")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        // 암호화 ON + 고정 키/시크릿 (prod 동등)
        registry.add("app.encryption.enabled", () -> "true");
        registry.add("app.encryption.key", () -> "dGhpc2lzYXRlc3RrZXlmb3JkZXZlbG9wbWVudG9ubHk=");
        registry.add("app.search.blind-index-secret",
                () -> "dGVzdC1ibGluZC1pbmRleC1zZWNyZXQtZm9yLWludGVncmF0aW9uLXRlc3Q=");
        // 백필 러너는 끈 채로 서비스를 직접 호출해 검증한다(러너는 단순 위임).
        registry.add("app.search.backfill.enabled", () -> "false");
    }

    @Autowired
    private SendMessageUseCase sendMessageUseCase;
    @Autowired
    private SearchMessageUseCase searchMessageUseCase;
    @Autowired
    private MessageSearchBackfillService backfillService;
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

    /** PR1 이전에 저장된 "기존 메시지" 상태 재현: 자동 적재된 토큰을 전부 비운다. */
    private void simulateLegacyMessagesWithoutTokens() {
        jdbcTemplate.update("DELETE FROM message_search_tokens");
    }

    private int tokenCount() {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM message_search_tokens", Integer.class);
        return c == null ? 0 : c;
    }

    @Test
    @DisplayName("토큰 없이 저장된 기존 메시지를 백필하면 과거 메시지가 다시 검색된다")
    void should_makeLegacyMessagesSearchable_afterBackfill() {
        Long sender = createUser("a@test.com", "철수");
        Long roomId = createRoomWithMembers(sender);

        sendMessageUseCase.sendMessage(roomId, sender, "오늘 비밀번호를 변경했어요");
        sendMessageUseCase.sendMessage(roomId, sender, "회의시간 안내드립니다");

        // 레거시 상태 재현 → 검색 0건 확인
        simulateLegacyMessagesWithoutTokens();
        assertThat(tokenCount()).isZero();
        assertThat(searchMessageUseCase.searchInChatRoom(roomId, sender, "비밀번호", 0, 20)).isEmpty();

        // 백필 실행
        BackfillResult result = backfillService.backfill(new BackfillOptions(500, 0L, false));

        // 과거 메시지가 다시 검색됨
        assertThat(result.scanned()).isEqualTo(2L);
        assertThat(result.indexed()).isEqualTo(2L);
        assertThat(tokenCount()).isGreaterThan(0);
        List<Message> hits = searchMessageUseCase.searchInChatRoom(roomId, sender, "비밀번호", 0, 20);
        assertThat(hits).extracting(Message::getContent).containsExactly("오늘 비밀번호를 변경했어요");
        assertThat(searchMessageUseCase.searchInChatRoom(roomId, sender, "회의시간", 0, 20)).hasSize(1);
    }

    @Test
    @DisplayName("백필을 재실행해도 중복/오류 없이 동일하게 검색된다 (idempotent)")
    void should_beIdempotent_onRerun() {
        Long sender = createUser("b@test.com", "영희");
        Long roomId = createRoomWithMembers(sender);
        sendMessageUseCase.sendMessage(roomId, sender, "재실행 안전 메시지 확인");
        simulateLegacyMessagesWithoutTokens();

        BackfillResult first = backfillService.backfill(new BackfillOptions(500, 0L, false));
        int afterFirst = tokenCount();
        BackfillResult second = backfillService.backfill(new BackfillOptions(500, 0L, false));
        int afterSecond = tokenCount();

        // 토큰 수가 늘어나지 않음(중복 없음) + 검색 정상
        assertThat(afterSecond).isEqualTo(afterFirst);
        assertThat(first.indexed()).isEqualTo(second.indexed());
        assertThat(searchMessageUseCase.searchInChatRoom(roomId, sender, "재실행", 0, 20)).hasSize(1);
    }

    @Test
    @DisplayName("skipExisting=true면 이미 토큰이 있는 메시지(신규 적재분)는 건너뛴다")
    void should_skipMessagesWithExistingTokens() {
        Long sender = createUser("c@test.com", "민수");
        Long roomId = createRoomWithMembers(sender);
        // 토큰이 이미 적재된 신규 메시지 (PR1 경로) — 비우지 않는다.
        sendMessageUseCase.sendMessage(roomId, sender, "토큰 있는 신규메시지 본문");

        BackfillResult result = backfillService.backfill(new BackfillOptions(500, 0L, true));

        assertThat(result.scanned()).isEqualTo(1L);
        assertThat(result.skipped()).isEqualTo(1L);
        assertThat(result.indexed()).isZero();
        assertThat(searchMessageUseCase.searchInChatRoom(roomId, sender, "신규메시지", 0, 20)).hasSize(1);
    }

    @Test
    @DisplayName("FILE/SYSTEM 메시지는 백필 대상에서 제외되고 TEXT만 색인된다")
    void should_indexOnlyTextMessages() {
        Long sender = createUser("d@test.com", "지수");
        Long roomId = createRoomWithMembers(sender);

        sendMessageUseCase.sendMessage(roomId, sender, "텍스트 메시지 본문");
        // FILE 메시지: content=파일명. SYSTEM: 시스템 메시지. 둘 다 직접 저장(senderId는 FK 충족 위해 실제 사용자).
        messageRepository.save(Message.builder()
                .id(idGenerator.nextId()).chatRoomId(roomId).senderId(sender)
                .content("문서파일이름.pdf").type(Message.MessageType.FILE)
                .fileUrl("https://example.com/f.pdf").fileName("문서파일이름.pdf").build());
        messageRepository.save(Message.builder()
                .id(idGenerator.nextId()).chatRoomId(roomId).senderId(sender)
                .content("시스템 입장 알림").type(Message.MessageType.SYSTEM).build());

        simulateLegacyMessagesWithoutTokens();
        BackfillResult result = backfillService.backfill(new BackfillOptions(500, 0L, false));

        // TEXT 1건만 스캔/색인 (FILE/SYSTEM 제외)
        assertThat(result.scanned()).isEqualTo(1L);
        assertThat(result.indexed()).isEqualTo(1L);
        assertThat(searchMessageUseCase.searchInChatRoom(roomId, sender, "텍스트", 0, 20)).hasSize(1);
        // 파일명은 백필되지 않아 검색되지 않음
        assertThat(searchMessageUseCase.searchInChatRoom(roomId, sender, "문서파일", 0, 20)).isEmpty();
    }

    @Test
    @DisplayName("소프트 삭제된 메시지는 백필 대상에서 제외된다")
    void should_excludeSoftDeletedMessages() {
        Long sender = createUser("e@test.com", "토끼");
        Long roomId = createRoomWithMembers(sender);

        Message alive = sendMessageUseCase.sendMessage(roomId, sender, "살아있는 회의시간");
        Message deleted = sendMessageUseCase.sendMessage(roomId, sender, "삭제된 회의시간");
        deleted.delete(java.time.LocalDateTime.now());
        messageRepository.save(deleted);

        simulateLegacyMessagesWithoutTokens();
        BackfillResult result = backfillService.backfill(new BackfillOptions(500, 0L, false));

        // 미삭제 1건만 백필
        assertThat(result.scanned()).isEqualTo(1L);
        List<Message> hits = searchMessageUseCase.searchInChatRoom(roomId, sender, "회의시간", 0, 20);
        assertThat(hits).extracting(Message::getId).containsExactly(alive.getId());
        assertThat(hits).extracting(Message::getId).doesNotContain(deleted.getId());
    }

    @Test
    @DisplayName("청크 크기보다 많은 메시지도 커서 청크 반복으로 모두 백필된다")
    void should_backfillAcrossMultipleChunks() {
        Long sender = createUser("f@test.com", "다람쥐");
        Long roomId = createRoomWithMembers(sender);
        for (int i = 0; i < 7; i++) {
            sendMessageUseCase.sendMessage(roomId, sender, "회의시간 청크테스트 " + i);
        }
        simulateLegacyMessagesWithoutTokens();

        // chunkSize=2 → 여러 청크에 걸쳐 처리
        BackfillResult result = backfillService.backfill(new BackfillOptions(2, 0L, false));

        assertThat(result.scanned()).isEqualTo(7L);
        assertThat(result.indexed()).isEqualTo(7L);
        assertThat(searchMessageUseCase.searchInChatRoom(roomId, sender, "청크테스트", 0, 20)).hasSize(7);
    }
}
