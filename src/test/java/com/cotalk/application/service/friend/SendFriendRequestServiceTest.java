package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.exception.InvalidFriendRequestException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SendFriendRequestServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @InjectMocks
    private SendFriendRequestService sendFriendRequestService;

    @Test
    @DisplayName("친구 요청 전송 성공")
    void should_sendFriendRequest_when_validRequest() {
        // given
        Long requesterId = 1L;
        Long receiverId = 2L;
        Long requestId = 100L;

        given(userRepository.findById(receiverId)).willReturn(java.util.Optional.of(
                com.cotalk.domain.entity.User.builder().id(receiverId).email("test@example.com").nickname("test").passwordHash("hash").build()
        ));
        given(friendRepository.existsByUserIdAndFriendId(requesterId, receiverId)).willReturn(false);
        given(friendRequestRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId)).willReturn(false);
        given(idGenerator.nextId()).willReturn(requestId);
        given(friendRequestRepository.save(any(FriendRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        Long result = sendFriendRequestService.sendFriendRequest(requesterId, receiverId);

        // then
        assertThat(result).isEqualTo(requestId);

        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestRepository).save(captor.capture());

        FriendRequest savedRequest = captor.getValue();
        assertThat(savedRequest.getRequesterId()).isEqualTo(requesterId);
        assertThat(savedRequest.getReceiverId()).isEqualTo(receiverId);
        assertThat(savedRequest.getStatus()).isEqualTo(FriendRequest.RequestStatus.PENDING);
    }

    @Test
    @DisplayName("자기 자신에게 친구 요청 시 예외 발생")
    void should_throwException_when_requestToSelf() {
        // given
        Long userId = 1L;

        // when & then
        assertThatThrownBy(() -> sendFriendRequestService.sendFriendRequest(userId, userId))
                .isInstanceOf(InvalidFriendRequestException.class);
    }

    @Test
    @DisplayName("존재하지 않는 사용자에게 친구 요청 시 예외 발생")
    void should_throwException_when_receiverNotFound() {
        // given
        Long requesterId = 1L;
        Long receiverId = 999L;

        given(userRepository.findById(receiverId)).willReturn(java.util.Optional.empty());

        // when & then
        assertThatThrownBy(() -> sendFriendRequestService.sendFriendRequest(requesterId, receiverId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("이미 친구인 경우 예외 발생")
    void should_throwException_when_alreadyFriends() {
        // given
        Long requesterId = 1L;
        Long receiverId = 2L;

        given(userRepository.findById(receiverId)).willReturn(java.util.Optional.of(
                com.cotalk.domain.entity.User.builder().id(receiverId).email("test@example.com").nickname("test").passwordHash("hash").build()
        ));
        given(friendRepository.existsByUserIdAndFriendId(requesterId, receiverId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> sendFriendRequestService.sendFriendRequest(requesterId, receiverId))
                .isInstanceOf(InvalidFriendRequestException.class);
    }

    @Test
    @DisplayName("이미 친구 요청을 보낸 경우 예외 발생")
    void should_throwException_when_requestAlreadySent() {
        // given
        Long requesterId = 1L;
        Long receiverId = 2L;

        given(userRepository.findById(receiverId)).willReturn(java.util.Optional.of(
                com.cotalk.domain.entity.User.builder().id(receiverId).email("test@example.com").nickname("test").passwordHash("hash").build()
        ));
        given(friendRepository.existsByUserIdAndFriendId(requesterId, receiverId)).willReturn(false);
        given(friendRequestRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> sendFriendRequestService.sendFriendRequest(requesterId, receiverId))
                .isInstanceOf(InvalidFriendRequestException.class);
    }
}
