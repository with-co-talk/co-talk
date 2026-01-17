package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.inbound.message.MarkAsReadUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarkAsReadServiceTest {

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    private ChatRoomMemberValidator chatRoomMemberValidator;

    private MarkAsReadUseCase markAsReadUseCase;

    @BeforeEach
    void setUp() {
        chatRoomMemberValidator = new ChatRoomMemberValidator(chatRoomMemberRepository);
        markAsReadUseCase = new MarkAsReadService(chatRoomMemberRepository, chatRoomMemberValidator);
    }

    @Test
    @DisplayName("읽음 표시 성공")
    void should_markAsRead_when_validRequest() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then
        assertThat(member.getLastReadAt()).isNotNull();
        verify(chatRoomMemberRepository).save(member);
    }

    @Test
    @DisplayName("채팅방 멤버가 아니면 예외 발생")
    void should_throwException_when_notMember() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> markAsReadUseCase.markAsRead(userId, chatRoomId))
                .isInstanceOf(ChatRoomAccessDeniedException.class);
    }

    @Test
    @DisplayName("읽음 시간이 현재 시간으로 업데이트됨")
    void should_updateLastReadAtToCurrentTime_when_markAsRead() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        LocalDateTime beforeMark = LocalDateTime.now().minusSeconds(1);
        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then
        assertThat(member.getLastReadAt()).isAfter(beforeMark);
    }
}
