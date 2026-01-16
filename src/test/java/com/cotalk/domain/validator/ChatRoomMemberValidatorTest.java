package com.cotalk.domain.validator;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@DisplayName("ChatRoomMemberValidator 테스트")
@ExtendWith(MockitoExtension.class)
class ChatRoomMemberValidatorTest {

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    private ChatRoomMemberValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ChatRoomMemberValidator(chatRoomMemberRepository);
    }

    @Test
    @DisplayName("채팅방 멤버이면 예외가 발생하지 않음")
    void should_notThrowException_when_userIsMember() {
        // given
        Long chatRoomId = 1L;
        Long userId = 100L;
        ChatRoomMember member = ChatRoomMember.builder()
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));

        // when & then
        assertThatCode(() -> validator.validateMembership(chatRoomId, userId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("채팅방 멤버가 아니면 예외 발생")
    void should_throwException_when_userIsNotMember() {
        // given
        Long chatRoomId = 1L;
        Long userId = 100L;

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> validator.validateMembership(chatRoomId, userId))
                .isInstanceOf(ChatRoomAccessDeniedException.class);
    }

    @Test
    @DisplayName("채팅방 멤버이면 멤버 객체를 반환함")
    void should_returnMember_when_userIsMember() {
        // given
        Long chatRoomId = 1L;
        Long userId = 100L;
        ChatRoomMember member = ChatRoomMember.builder()
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));

        // when
        ChatRoomMember result = validator.getMemberOrThrow(chatRoomId, userId);

        // then
        assertThatCode(() -> {
            if (result == null) throw new AssertionError("result should not be null");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("채팅방 멤버가 아니면 멤버 조회 시 예외 발생")
    void should_throwException_when_getMemberAndNotMember() {
        // given
        Long chatRoomId = 1L;
        Long userId = 100L;

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> validator.getMemberOrThrow(chatRoomId, userId))
                .isInstanceOf(ChatRoomAccessDeniedException.class);
    }
}
