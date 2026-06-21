package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.mapper.MessageMapper;
import com.cotalk.adapter.outbound.persistence.message.MessageRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.message.MessageSearchTokenJpaEntity;
import com.cotalk.adapter.outbound.persistence.message.MessageSearchTokenJpaRepository;
import com.cotalk.adapter.outbound.persistence.message.MessageSearchTokenRepositoryAdapter;
import com.cotalk.domain.entity.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.cotalk.infrastructure.config.JpaAuditingConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 블라인드 인덱스 토큰 조인 검색 쿼리 회귀 테스트 (@DataJpaTest, H2).
 *
 * <p>암호화/HMAC와 무관하게 토큰 조인 SQL의 AND 매칭 정합성만 검증한다. 평문 토큰을
 * 직접 {@code message_search_tokens}에 적재하고, "키워드의 모든 토큰을 포함하는 메시지"
 * ({@code COUNT(DISTINCT token) = :tokenCount})만 조회되는지 본다.</p>
 *
 * <p>참고: H2(ddl-auto)로 스키마를 만들고 Flyway는 비활성이다. prod 동등(실제 Flyway +
 * 암호화 ON) 검증은 testcontainers 통합테스트가 담당한다.</p>
 *
 * @author seunggu.lee
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({MessageRepositoryAdapter.class, MessageMapper.class, MessageSearchTokenRepositoryAdapter.class, JpaAuditingConfig.class})
@DisplayName("MessageJpaRepository 토큰 조인 검색")
class MessageJpaRepositoryTest {

    @Autowired
    private MessageRepositoryAdapter messageRepository;

    @Autowired
    private MessageSearchTokenRepositoryAdapter tokenRepository;

    @Autowired
    private MessageSearchTokenJpaRepository tokenJpaRepository;

    private long nextId = 1L;

    private Message saveText(Long chatRoomId, Long senderId, String content, String... tokens) {
        Message saved = messageRepository.save(Message.builder()
                .id(nextId++)
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(content)
                .type(Message.MessageType.TEXT)
                .build());
        for (String t : tokens) {
            tokenJpaRepository.save(MessageSearchTokenJpaEntity.of(saved.getId(), t));
        }
        return saved;
    }

    @Nested
    @DisplayName("채팅방 내 토큰 검색 시")
    class SearchInChatRoom {

        @Test
        @DisplayName("키워드의 모든 토큰을 포함하는 메시지만 매칭한다 (AND 매칭)")
        void should_matchOnlyWhenAllTokensPresent() {
            // given: 메시지 A는 두 토큰 모두, 메시지 B는 한 토큰만 보유
            Message a = saveText(1L, 10L, "안녕하세요 반갑습니다", "t-an", "t-ny");
            saveText(1L, 11L, "안녕 친구", "t-an");

            // when: 두 토큰을 모두 포함하는 메시지 검색 (tokenCount=2)
            List<Message> results = messageRepository.searchByTokensInChatRoom(
                    1L, List.of("t-an", "t-ny"), 2, 0, 20);

            // then: A만 매칭
            assertThat(results).extracting(Message::getId).containsExactly(a.getId());
        }

        @Test
        @DisplayName("다른 채팅방의 메시지는 검색되지 않는다")
        void should_notMatch_when_differentChatRoom() {
            Message room1 = saveText(1L, 10L, "공통키워드 방1", "t-com");
            saveText(2L, 11L, "공통키워드 방2", "t-com");

            List<Message> results = messageRepository.searchByTokensInChatRoom(
                    1L, List.of("t-com"), 1, 0, 20);

            assertThat(results).extracting(Message::getChatRoomId).containsExactly(1L);
            assertThat(results).extracting(Message::getId).containsExactly(room1.getId());
        }

        @Test
        @DisplayName("소프트 삭제된 메시지는 토큰이 남아 있어도 제외된다")
        void should_excludeSoftDeleted() {
            Message alive = saveText(1L, 10L, "살아있는 메시지", "t-x", "t-y");
            Message deleted = Message.builder()
                    .id(nextId++).chatRoomId(1L).senderId(11L).content("삭제됨")
                    .type(Message.MessageType.TEXT).deleted(true).build();
            messageRepository.save(deleted);
            tokenJpaRepository.save(MessageSearchTokenJpaEntity.of(deleted.getId(), "t-x"));
            tokenJpaRepository.save(MessageSearchTokenJpaEntity.of(deleted.getId(), "t-y"));

            List<Message> results = messageRepository.searchByTokensInChatRoom(
                    1L, List.of("t-x", "t-y"), 2, 0, 20);

            assertThat(results).extracting(Message::getId).containsExactly(alive.getId());
        }
    }

    @Nested
    @DisplayName("전체 채팅방 토큰 검색 시")
    class SearchAcrossChatRooms {

        @Test
        @DisplayName("빈 토큰 집합이면 빈 결과를 반환한다")
        void should_returnEmpty_when_noTokens() {
            saveText(1L, 10L, "내용", "t-a");
            List<Message> results = messageRepository.searchByTokensInUserChatRooms(
                    10L, List.of(), 0, 0, 20);
            assertThat(results).isEmpty();
        }
    }
}
