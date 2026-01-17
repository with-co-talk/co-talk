package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.friend.RejectFriendRequestUseCase;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RejectFriendRequestServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    private RejectFriendRequestUseCase rejectFriendRequestUseCase;

    @BeforeEach
    void setUp() {
        rejectFriendRequestUseCase = new RejectFriendRequestService(friendRequestRepository);
    }

    @Test
    @DisplayName("친구 요청 거절 성공")
    void should_rejectFriendRequest_when_validRequest() {
        // given
        Long userId = 2L;
        Long requestId = 100L;
        FriendRequest friendRequest = FriendRequest.builder()
                .id(requestId)
                .requesterId(1L)
                .receiverId(userId)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(friendRequest));

        // when
        rejectFriendRequestUseCase.rejectFriendRequest(userId, requestId);

        // then
        assertThat(friendRequest.getStatus()).isEqualTo(FriendRequest.RequestStatus.REJECTED);
        verify(friendRequestRepository).save(friendRequest);
    }

    @Test
    @DisplayName("존재하지 않는 친구 요청 거절 시 예외 발생")
    void should_throwException_when_requestNotFound() {
        // given
        Long userId = 2L;
        Long requestId = 999L;
        given(friendRequestRepository.findById(requestId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> rejectFriendRequestUseCase.rejectFriendRequest(userId, requestId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("친구 요청을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("본인이 받은 요청이 아닌 경우 거절 불가")
    void should_throwException_when_notReceiver() {
        // given
        Long userId = 3L; // 실제 수신자가 아닌 사용자
        Long requestId = 100L;
        FriendRequest friendRequest = FriendRequest.builder()
                .id(requestId)
                .requesterId(1L)
                .receiverId(2L) // 실제 수신자
                .status(FriendRequest.RequestStatus.PENDING)
                .build();

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(friendRequest));

        // when & then
        assertThatThrownBy(() -> rejectFriendRequestUseCase.rejectFriendRequest(userId, requestId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("권한이 없습니다");
    }

    @Test
    @DisplayName("이미 처리된 요청 거절 시 예외 발생")
    void should_throwException_when_alreadyProcessed() {
        // given
        Long userId = 2L;
        Long requestId = 100L;
        FriendRequest friendRequest = FriendRequest.builder()
                .id(requestId)
                .requesterId(1L)
                .receiverId(userId)
                .status(FriendRequest.RequestStatus.ACCEPTED) // 이미 수락됨
                .build();

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(friendRequest));

        // when & then
        assertThatThrownBy(() -> rejectFriendRequestUseCase.rejectFriendRequest(userId, requestId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("대기 중인 요청만 거절할 수 있습니다");
    }
}
