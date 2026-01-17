package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.exception.FriendNotFoundException;
import com.cotalk.domain.exception.InvalidFriendRequestException;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AcceptFriendRequestServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @InjectMocks
    private AcceptFriendRequestService acceptFriendRequestService;

    @Test
    @DisplayName("친구 요청 수락 성공")
    void should_acceptFriendRequest_when_validRequest() {
        // given
        Long receiverId = 2L;
        Long requestId = 100L;
        Long requesterId = 1L;
        Long friendId1 = 200L;
        Long friendId2 = 201L;

        FriendRequest friendRequest = FriendRequest.builder()
                .id(requestId)
                .requesterId(requesterId)
                .receiverId(receiverId)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(friendRequest));
        given(idGenerator.nextId())
                .willReturn(friendId1)
                .willReturn(friendId2);
        given(friendRepository.save(any(Friend.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        Long result = acceptFriendRequestService.acceptFriendRequest(receiverId, requestId);

        // then
        assertThat(result).isEqualTo(friendId1);

        ArgumentCaptor<Friend> captor = ArgumentCaptor.forClass(Friend.class);
        verify(friendRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues()).hasSize(2);
    }

    @Test
    @DisplayName("존재하지 않는 친구 요청 수락 시 예외 발생")
    void should_throwException_when_requestNotFound() {
        // given
        Long receiverId = 2L;
        Long requestId = 999L;

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> acceptFriendRequestService.acceptFriendRequest(receiverId, requestId))
                .isInstanceOf(FriendNotFoundException.class);
    }

    @Test
    @DisplayName("본인이 받은 요청이 아닌 경우 예외 발생")
    void should_throwException_when_notReceiver() {
        // given
        Long receiverId = 3L;
        Long requestId = 100L;

        FriendRequest friendRequest = FriendRequest.builder()
                .id(requestId)
                .requesterId(1L)
                .receiverId(2L)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(friendRequest));

        // when & then
        assertThatThrownBy(() -> acceptFriendRequestService.acceptFriendRequest(receiverId, requestId))
                .isInstanceOf(InvalidFriendRequestException.class);
    }

    @Test
    @DisplayName("이미 처리된 요청 수락 시 예외 발생")
    void should_throwException_when_alreadyProcessed() {
        // given
        Long receiverId = 2L;
        Long requestId = 100L;

        FriendRequest friendRequest = FriendRequest.builder()
                .id(requestId)
                .requesterId(1L)
                .receiverId(receiverId)
                .status(FriendRequest.RequestStatus.ACCEPTED)
                .build();

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(friendRequest));

        // when & then
        assertThatThrownBy(() -> acceptFriendRequestService.acceptFriendRequest(receiverId, requestId))
                .isInstanceOf(InvalidFriendRequestException.class);
    }
}
