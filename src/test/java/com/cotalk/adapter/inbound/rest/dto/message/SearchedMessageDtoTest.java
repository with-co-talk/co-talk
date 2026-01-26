package com.cotalk.adapter.inbound.rest.dto.message;

import com.cotalk.domain.entity.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SearchedMessageDto")
class SearchedMessageDtoTest {

    @Nested
    @DisplayName("from 메서드")
    class FromMethod {

        @Test
        @DisplayName("Message 엔티티로부터 SearchedMessageDto를 생성할 수 있다")
        void should_createDto_when_fromMessage() {
            // given
            LocalDateTime createdAt = LocalDateTime.now();
            Message message = Message.builder()
                    .id(100L)
                    .chatRoomId(200L)
                    .senderId(300L)
                    .content("검색된 메시지")
                    .type(Message.MessageType.TEXT)
                    .build();
            ReflectionTestUtils.setField(message, "createdAt", createdAt);

            // when
            SearchedMessageDto dto = SearchedMessageDto.from(message);

            // then
            assertThat(dto.id()).isEqualTo(100L);
            assertThat(dto.chatRoomId()).isEqualTo(200L);
            assertThat(dto.senderId()).isEqualTo(300L);
            assertThat(dto.content()).isEqualTo("검색된 메시지");
            assertThat(dto.type()).isEqualTo("TEXT");
            assertThat(dto.createdAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("파일 메시지도 변환할 수 있다")
        void should_createDto_when_fromFileMessage() {
            // given
            LocalDateTime createdAt = LocalDateTime.now();
            Message message = Message.builder()
                    .id(100L)
                    .chatRoomId(200L)
                    .senderId(300L)
                    .content(null)
                    .type(Message.MessageType.FILE)
                    .fileUrl("https://example.com/file.pdf")
                    .fileName("document.pdf")
                    .build();
            ReflectionTestUtils.setField(message, "createdAt", createdAt);

            // when
            SearchedMessageDto dto = SearchedMessageDto.from(message);

            // then
            assertThat(dto.type()).isEqualTo("FILE");
            assertThat(dto.content()).isNull();
        }

        @Test
        @DisplayName("message가 null인 경우 NullPointerException 발생")
        void should_throwException_when_messageIsNull() {
            // when & then
            assertThatThrownBy(() -> SearchedMessageDto.from(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
