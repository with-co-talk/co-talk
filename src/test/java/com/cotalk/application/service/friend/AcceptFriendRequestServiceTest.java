package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.exception.FriendNotFoundException;
import com.cotalk.domain.exception.InvalidFriendRequestException;
import com.cotalk.domain.port.outbound.DistributedLockPort;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
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

    @Mock
    private DistributedLockPort lockExecutor;

    @Mock
    private TransactionTemplate transactionTemplate;

    private AcceptFriendRequestService acceptFriendRequestService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        acceptFriendRequestService = new AcceptFriendRequestService(
                friendRequestRepository, friendRepository, idGenerator, lockExecutor, transactionTemplate);

        // 분산락 모킹: 락 획득 후 바로 실행
        lenient().when(lockExecutor.executeWithLock(anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(1);
                    return supplier.get();
                });

        // TransactionTemplate 모킹: 트랜잭션 콜백 바로 실행
        lenient().when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
    }

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
