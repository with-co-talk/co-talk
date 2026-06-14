package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.message.MessageRepositoryAdapter;
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
 * 메시지 키워드 검색 동작 회귀 테스트.
 *
 * <p>이 테스트는 메시지 검색이 부분 일치(substring) LIKE 방식으로 동작함을 고정한다.
 * 전문 검색(단어 단위)으로 전환하면 깨졌을 케이스(단어 중간 부분 매칭 등)를 명시적으로
 * 검증하여 검색 UX 계약을 보장한다.</p>
 *
 * <p>참고: 테스트 환경은 H2(ddl-auto)로 스키마를 만들고 Flyway는 비활성이므로
 * GIN 인덱스가 애초에 존재하지 않는다. 따라서 'V15의 GIN 제거 무회귀'를 직접 보장하는
 * 것은 이 테스트가 아니다. 회귀 안전성의 근거는 '검색 쿼리가 선행 와일드카드 LIKE라
 * 어떤 GIN도 사용하지 않는다'는 정적 사실이며(V15 마이그레이션 주석 참조), 이 테스트는
 * 그 LIKE substring 동작 자체를 고정하는 역할로 한정된다.</p>
 *
 * @author seunggu.lee
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({MessageRepositoryAdapter.class, JpaAuditingConfig.class})
@DisplayName("MessageJpaRepository 키워드 검색")
class MessageJpaRepositoryTest {

    @Autowired
    private MessageRepositoryAdapter messageRepository;

    private long nextId = 1L;

    private Message text(Long chatRoomId, Long senderId, String content) {
        return Message.builder()
                .id(nextId++)
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(content)
                .type(Message.MessageType.TEXT)
                .build();
    }

    @Nested
    @DisplayName("채팅방 내 검색 시")
    class SearchInChatRoom {

        @Test
        @DisplayName("단어 중간의 부분 문자열도 매칭한다 (substring UX 유지)")
        void should_matchSubstring_when_keywordInsideWord() {
            // given
            messageRepository.save(text(1L, 10L, "안녕하세요 반갑습니다"));
            messageRepository.save(text(1L, 11L, "오늘 점심 뭐 먹지"));

            // when: "녕하"는 "안녕하세요" 단어의 중간 부분 문자열 → FTS였다면 매칭 실패
            List<Message> results = messageRepository.searchByKeywordInChatRoom(1L, "녕하", 0, 20);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getContent()).isEqualTo("안녕하세요 반갑습니다");
        }

        @Test
        @DisplayName("대소문자를 구분하지 않는다")
        void should_matchCaseInsensitively() {
            // given
            messageRepository.save(text(1L, 10L, "Hello World"));

            // when
            List<Message> results = messageRepository.searchByKeywordInChatRoom(1L, "hello", 0, 20);

            // then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("다른 채팅방의 메시지는 검색되지 않는다")
        void should_notMatch_when_differentChatRoom() {
            // given
            messageRepository.save(text(1L, 10L, "공통키워드 방1"));
            messageRepository.save(text(2L, 11L, "공통키워드 방2"));

            // when
            List<Message> results = messageRepository.searchByKeywordInChatRoom(1L, "공통키워드", 0, 20);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getChatRoomId()).isEqualTo(1L);
        }
    }
}
