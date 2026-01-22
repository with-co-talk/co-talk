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
class GetReceivedFriendRequestsServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @InjectMocks
    private GetReceivedFriendRequestsService getReceivedFriendRequestsService;

    @Test
    @DisplayName("받은 친구 요청 목록 조회 성공")
    void should_returnReceivedFriendRequests_when_validReceiverId() {
        // given
        Long receiverId = 1L;

        FriendRequest request1 = FriendRequest.builder()
                .id(100L)
                .requesterId(2L)
                .receiverId(receiverId)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();

        FriendRequest request2 = FriendRequest.builder()
                .id(101L)
                .requesterId(3L)
                .receiverId(receiverId)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();

        List<FriendRequest> requests = List.of(request1, request2);
        given(friendRequestRepository.findPendingByReceiverId(receiverId)).willReturn(requests);

        // when
        List<FriendRequest> result = getReceivedFriendRequestsService.getReceivedFriendRequests(receiverId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(100L);
        assertThat(result.get(1).getId()).isEqualTo(101L);
        assertThat(result.get(0).getReceiverId()).isEqualTo(receiverId);
        assertThat(result.get(1).getReceiverId()).isEqualTo(receiverId);
    }

    @Test
    @DisplayName("받은 친구 요청이 없을 때 빈 리스트 반환")
    void should_returnEmptyList_when_noReceivedRequests() {
        // given
        Long receiverId = 1L;
        given(friendRequestRepository.findPendingByReceiverId(receiverId)).willReturn(List.of());

        // when
        List<FriendRequest> result = getReceivedFriendRequestsService.getReceivedFriendRequests(receiverId);

        // then
        assertThat(result).isEmpty();
    }
}
