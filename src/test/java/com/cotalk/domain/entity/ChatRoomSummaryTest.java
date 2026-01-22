package com.cotalk.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChatRoomSummary 단위 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("ChatRoomSummary")
class ChatRoomSummaryTest {

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("1:1 채팅방 요약 정보를 생성할 수 있다")
        void should_createDirectChatRoomSummary() {
            // given
            LocalDateTime now = LocalDateTime.now();

            // when
            ChatRoomSummary summary = new ChatRoomSummary(
                    1L,
                    "1:1 채팅",
                    ChatRoom.ChatRoomType.DIRECT,
                    now,
                    "마지막 메시지",
                    now,
                    5,
                    2L,
                    "상대방",
                    "https://example.com/avatar.jpg"
            );

            // then
            assertThat(summary.id()).isEqualTo(1L);
            assertThat(summary.name()).isEqualTo("1:1 채팅");
            assertThat(summary.type()).isEqualTo(ChatRoom.ChatRoomType.DIRECT);
            assertThat(summary.createdAt()).isEqualTo(now);
            assertThat(summary.lastMessage()).isEqualTo("마지막 메시지");
            assertThat(summary.lastMessageAt()).isEqualTo(now);
            assertThat(summary.unreadCount()).isEqualTo(5);
            assertThat(summary.otherUserId()).isEqualTo(2L);
            assertThat(summary.otherUserNickname()).isEqualTo("상대방");
            assertThat(summary.otherUserAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
        }

        @Test
        @DisplayName("그룹 채팅방 요약 정보를 생성할 수 있다")
        void should_createGroupChatRoomSummary() {
            // given
            LocalDateTime now = LocalDateTime.now();

            // when
            ChatRoomSummary summary = new ChatRoomSummary(
                    2L,
                    "프로젝트 팀",
                    ChatRoom.ChatRoomType.GROUP,
                    now,
                    "안녕하세요",
                    now,
                    10,
                    null,
                    null,
                    null
            );

            // then
            assertThat(summary.id()).isEqualTo(2L);
            assertThat(summary.name()).isEqualTo("프로젝트 팀");
            assertThat(summary.type()).isEqualTo(ChatRoom.ChatRoomType.GROUP);
            assertThat(summary.unreadCount()).isEqualTo(10);
            assertThat(summary.otherUserId()).isNull();
            assertThat(summary.otherUserNickname()).isNull();
            assertThat(summary.otherUserAvatarUrl()).isNull();
        }

        @Test
        @DisplayName("메시지가 없는 채팅방 요약 정보를 생성할 수 있다")
        void should_createSummary_when_noMessages() {
            // given
            LocalDateTime now = LocalDateTime.now();

            // when
            ChatRoomSummary summary = new ChatRoomSummary(
                    3L,
                    "빈 채팅방",
                    ChatRoom.ChatRoomType.DIRECT,
                    now,
                    null,
                    null,
                    0,
                    4L,
                    "새친구",
                    null
            );

            // then
            assertThat(summary.lastMessage()).isNull();
            assertThat(summary.lastMessageAt()).isNull();
            assertThat(summary.unreadCount()).isZero();
        }
    }

    @Nested
    @DisplayName("record 특성")
    class RecordCharacteristics {

        @Test
        @DisplayName("동일한 값을 가진 record는 equals가 true")
        void should_beEqual_when_sameValues() {
            // given
            LocalDateTime now = LocalDateTime.now();

            ChatRoomSummary summary1 = new ChatRoomSummary(
                    1L, "채팅방", ChatRoom.ChatRoomType.DIRECT, now,
                    "메시지", now, 0, 2L, "닉네임", null
            );
            ChatRoomSummary summary2 = new ChatRoomSummary(
                    1L, "채팅방", ChatRoom.ChatRoomType.DIRECT, now,
                    "메시지", now, 0, 2L, "닉네임", null
            );

            // then
            assertThat(summary1).isEqualTo(summary2);
            assertThat(summary1.hashCode()).isEqualTo(summary2.hashCode());
        }

        @Test
        @DisplayName("다른 값을 가진 record는 equals가 false")
        void should_notBeEqual_when_differentValues() {
            // given
            LocalDateTime now = LocalDateTime.now();

            ChatRoomSummary summary1 = new ChatRoomSummary(
                    1L, "채팅방1", ChatRoom.ChatRoomType.DIRECT, now,
                    "메시지", now, 0, 2L, "닉네임", null
            );
            ChatRoomSummary summary2 = new ChatRoomSummary(
                    2L, "채팅방2", ChatRoom.ChatRoomType.GROUP, now,
                    "메시지", now, 5, null, null, null
            );

            // then
            assertThat(summary1).isNotEqualTo(summary2);
        }

        @Test
        @DisplayName("toString은 record 내용을 포함한다")
        void should_containsContent_when_toString() {
            // given
            ChatRoomSummary summary = new ChatRoomSummary(
                    1L, "테스트 채팅방", ChatRoom.ChatRoomType.DIRECT, LocalDateTime.now(),
                    null, null, 0, 2L, "테스터", null
            );

            // when
            String result = summary.toString();

            // then
            assertThat(result).contains("테스트 채팅방");
            assertThat(result).contains("DIRECT");
            assertThat(result).contains("테스터");
        }
    }
}
