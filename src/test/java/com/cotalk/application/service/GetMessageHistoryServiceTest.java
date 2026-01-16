package com.cotalk.application.service;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetMessageHistoryServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    private ChatRoomMemberValidator chatRoomMemberValidator;

    private GetMessageHistoryService getMessageHistoryService;

    @BeforeEach
    void setUp() {
        chatRoomMemberValidator = new ChatRoomMemberValidator(chatRoomMemberRepository);
        getMessageHistoryService = new GetMessageHistoryService(messageRepository, chatRoomMemberValidator);
    }

    @Test
    @DisplayName("커서 기반 메시지 조회 - 최신 메시지부터 (beforeMessageId가 null)")
    void should_returnLatestMessages_when_beforeMessageIdIsNull() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        int size = 20;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(1L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        List<Message> messages = List.of(
                Message.builder()
                        .id(1000L)
                        .chatRoomId(chatRoomId)
                        .senderId(userId)
                        .content("최신 메시지")
                        .createdAt(LocalDateTime.now())
                        .build(),
                Message.builder()
                        .id(999L)
                        .chatRoomId(chatRoomId)
                        .senderId(2L)
                        .content("이전 메시지")
                        .createdAt(LocalDateTime.now().minusMinutes(1))
                        .build()
        );

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findByChatRoomIdBeforeMessageId(chatRoomId, null, size))
                .willReturn(messages);

        // when
        List<Message> result = getMessageHistoryService.getMessageHistory(chatRoomId, userId, null, size);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1000L);
        assertThat(result.get(0).getContent()).isEqualTo("최신 메시지");
    }

    @Test
    @DisplayName("커서 기반 메시지 조회 - 특정 메시지 이전부터 (위로 스크롤)")
    void should_returnOlderMessages_when_beforeMessageIdProvided() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        Long beforeMessageId = 1000L;
        int size = 20;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(1L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        List<Message> messages = List.of(
                Message.builder()
                        .id(999L)
                        .chatRoomId(chatRoomId)
                        .senderId(2L)
                        .content("이전 메시지 1")
                        .createdAt(LocalDateTime.now().minusMinutes(1))
                        .build(),
                Message.builder()
                        .id(998L)
                        .chatRoomId(chatRoomId)
                        .senderId(userId)
                        .content("이전 메시지 2")
                        .createdAt(LocalDateTime.now().minusMinutes(2))
                        .build()
        );

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findByChatRoomIdBeforeMessageId(chatRoomId, beforeMessageId, size))
                .willReturn(messages);

        // when
        List<Message> result = getMessageHistoryService.getMessageHistory(chatRoomId, userId, beforeMessageId, size);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(999L);
        assertThat(result.get(1).getId()).isEqualTo(998L);
    }

    @Test
    @DisplayName("채팅방 멤버가 아닌 경우 예외 발생")
    void should_throwException_when_userIsNotMember() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        int size = 20;

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getMessageHistoryService.getMessageHistory(chatRoomId, userId, null, size))
                .isInstanceOf(ChatRoomAccessDeniedException.class);
    }
}
