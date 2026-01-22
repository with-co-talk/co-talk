package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetSentFriendRequestsServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @InjectMocks
    private GetSentFriendRequestsService getSentFriendRequestsService;

    @Test
    @DisplayName("보낸 친구 요청 목록 조회 성공")
    void should_returnSentFriendRequests_when_validRequesterId() {
        // given
        Long requesterId = 1L;

        FriendRequest request1 = FriendRequest.builder()
                .id(100L)
                .requesterId(requesterId)
                .receiverId(2L)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();

        FriendRequest request2 = FriendRequest.builder()
                .id(101L)
                .requesterId(requesterId)
                .receiverId(3L)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();

        List<FriendRequest> requests = List.of(request1, request2);
        given(friendRequestRepository.findPendingByRequesterId(requesterId)).willReturn(requests);

        // when
        List<FriendRequest> result = getSentFriendRequestsService.getSentFriendRequests(requesterId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(100L);
        assertThat(result.get(1).getId()).isEqualTo(101L);
        assertThat(result.get(0).getRequesterId()).isEqualTo(requesterId);
        assertThat(result.get(1).getRequesterId()).isEqualTo(requesterId);
    }

    @Test
    @DisplayName("보낸 친구 요청이 없을 때 빈 리스트 반환")
    void should_returnEmptyList_when_noSentRequests() {
        // given
        Long requesterId = 1L;
        given(friendRequestRepository.findPendingByRequesterId(requesterId)).willReturn(List.of());

        // when
        List<FriendRequest> result = getSentFriendRequestsService.getSentFriendRequests(requesterId);

        // then
        assertThat(result).isEmpty();
    }
}
