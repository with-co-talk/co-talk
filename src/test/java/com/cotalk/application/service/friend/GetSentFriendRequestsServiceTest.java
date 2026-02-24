package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    @Test
    @DisplayName("Pageable을 사용한 보낸 친구 요청 목록 DB 레벨 페이지네이션 조회 성공")
    void should_returnPagedSentFriendRequests_when_pageableProvided() {
        // given
        Long requesterId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        FriendRequest request1 = FriendRequest.builder()
                .id(100L)
                .requesterId(requesterId)
                .receiverId(2L)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();

        Page<FriendRequest> requestPage = new PageImpl<>(List.of(request1), pageable, 1);
        given(friendRequestRepository.findPendingByRequesterId(requesterId, pageable)).willReturn(requestPage);

        // when
        Page<FriendRequest> result = getSentFriendRequestsService.getSentFriendRequests(requesterId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(100L);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Pageable을 사용한 두 번째 페이지 조회 성공")
    void should_returnSecondPage_when_pageableWithOffset() {
        // given
        Long requesterId = 1L;
        Pageable pageable = PageRequest.of(1, 5);

        FriendRequest request = FriendRequest.builder()
                .id(106L)
                .requesterId(requesterId)
                .receiverId(7L)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();

        Page<FriendRequest> requestPage = new PageImpl<>(List.of(request), pageable, 10);
        given(friendRequestRepository.findPendingByRequesterId(requesterId, pageable)).willReturn(requestPage);

        // when
        Page<FriendRequest> result = getSentFriendRequestsService.getSentFriendRequests(requesterId, pageable);

        // then
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(10);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }
}
