package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.exception.InvalidFriendRequestException;
import com.cotalk.domain.exception.SelfActionNotAllowedException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import com.cotalk.domain.port.outbound.NotificationCommandPort;
import com.cotalk.domain.validator.UserValidator;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import com.cotalk.infrastructure.lock.DistributedLockExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SendFriendRequestServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private UserValidator userValidator;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @Mock
    private DistributedLockExecutor lockExecutor;

    @Mock
    private NotificationCommandPort notificationCommandPort;

    @Mock
    private TransactionTemplate transactionTemplate;

    private SendFriendRequestService sendFriendRequestService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        sendFriendRequestService = new SendFriendRequestService(
                friendRequestRepository, friendRepository, userValidator, idGenerator, lockExecutor,
                notificationCommandPort, transactionTemplate);

        // 분산락 모킹: 락 획득 후 바로 실행 (lenient로 불필요한 stub 경고 방지)
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
    @DisplayName("친구 요청 전송 성공")
    void should_sendFriendRequest_when_validRequest() {
        // given
        Long requesterId = 1L;
        Long receiverId = 2L;
        Long requestId = 100L;

        doNothing().when(userValidator).validateNotSelfAction(requesterId, receiverId, "친구 요청");
        given(userValidator.validateUserExists(receiverId)).willReturn(
                User.builder().id(receiverId).email(new Email("test@example.com")).nickname("test").passwordHash("hash").build()
        );
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
        willThrow(new SelfActionNotAllowedException("친구 요청"))
                .given(userValidator).validateNotSelfAction(userId, userId, "친구 요청");

        // when & then
        assertThatThrownBy(() -> sendFriendRequestService.sendFriendRequest(userId, userId))
                .isInstanceOf(SelfActionNotAllowedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 사용자에게 친구 요청 시 예외 발생")
    void should_throwException_when_receiverNotFound() {
        // given
        Long requesterId = 1L;
        Long receiverId = 999L;

        doNothing().when(userValidator).validateNotSelfAction(requesterId, receiverId, "친구 요청");
        willThrow(new UserNotFoundException(receiverId))
                .given(userValidator).validateUserExists(receiverId);

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

        doNothing().when(userValidator).validateNotSelfAction(requesterId, receiverId, "친구 요청");
        given(userValidator.validateUserExists(receiverId)).willReturn(
                User.builder().id(receiverId).email(new Email("test@example.com")).nickname("test").passwordHash("hash").build()
        );
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

        doNothing().when(userValidator).validateNotSelfAction(requesterId, receiverId, "친구 요청");
        given(userValidator.validateUserExists(receiverId)).willReturn(
                User.builder().id(receiverId).email(new Email("test@example.com")).nickname("test").passwordHash("hash").build()
        );
        given(friendRepository.existsByUserIdAndFriendId(requesterId, receiverId)).willReturn(false);
        given(friendRequestRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> sendFriendRequestService.sendFriendRequest(requesterId, receiverId))
                .isInstanceOf(InvalidFriendRequestException.class);
    }

    @Test
    @DisplayName("친구 요청 전송 시 FCM 푸시 알림 전송")
    void should_sendPushNotification_when_friendRequestSent() {
        // given
        Long requesterId = 1L;
        Long receiverId = 2L;
        Long requestId = 100L;
        String requesterNickname = "요청자닉네임";

        User requester = User.builder()
                .id(requesterId)
                .email(new Email("requester@example.com"))
                .nickname(requesterNickname)
                .passwordHash("hash")
                .build();

        User receiver = User.builder()
                .id(receiverId)
                .email(new Email("receiver@example.com"))
                .nickname("수신자닉네임")
                .passwordHash("hash")
                .build();

        doNothing().when(userValidator).validateNotSelfAction(requesterId, receiverId, "친구 요청");
        given(userValidator.validateUserExists(receiverId)).willReturn(receiver);
        given(userValidator.validateUserExists(requesterId)).willReturn(requester);
        given(friendRepository.existsByUserIdAndFriendId(requesterId, receiverId)).willReturn(false);
        given(friendRequestRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId)).willReturn(false);
        given(friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(
                receiverId, requesterId, FriendRequest.RequestStatus.PENDING)).willReturn(false);
        given(idGenerator.nextId()).willReturn(requestId);
        given(friendRequestRepository.save(any(FriendRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        sendFriendRequestService.sendFriendRequest(requesterId, receiverId);

        // then
        verify(notificationCommandPort).sendFriendRequestNotification(receiverId, requesterNickname);
    }

    @Test
    @DisplayName("상대방이 이미 나에게 요청을 보낸 경우 예외 발생")
    void should_throwException_when_oppositeRequestExists() {
        // given
        Long requesterId = 1L;
        Long receiverId = 2L;

        doNothing().when(userValidator).validateNotSelfAction(requesterId, receiverId, "친구 요청");
        given(userValidator.validateUserExists(receiverId)).willReturn(
                User.builder().id(receiverId).email(new Email("test@example.com")).nickname("test").passwordHash("hash").build()
        );
        given(friendRepository.existsByUserIdAndFriendId(requesterId, receiverId)).willReturn(false);
        given(friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(
                receiverId, requesterId, FriendRequest.RequestStatus.PENDING)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> sendFriendRequestService.sendFriendRequest(requesterId, receiverId))
                .isInstanceOf(InvalidFriendRequestException.class)
                .hasMessageContaining("상대방이 이미 친구 요청을 보냈습니다");
    }

    @Test
    @DisplayName("동시성 제어: DataIntegrityViolationException 발생 시 예외 처리")
    void should_handleDataIntegrityViolation_when_concurrentRequest() {
        // given
        Long requesterId = 1L;
        Long receiverId = 2L;
        Long requestId = 100L;

        doNothing().when(userValidator).validateNotSelfAction(requesterId, receiverId, "친구 요청");
        given(userValidator.validateUserExists(receiverId)).willReturn(
                User.builder().id(receiverId).email(new Email("test@example.com")).nickname("test").passwordHash("hash").build()
        );
        given(friendRepository.existsByUserIdAndFriendId(requesterId, receiverId)).willReturn(false);
        given(friendRequestRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId)).willReturn(false);
        given(friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(
                receiverId, requesterId, FriendRequest.RequestStatus.PENDING)).willReturn(false);
        given(idGenerator.nextId()).willReturn(requestId);
        given(friendRequestRepository.save(any(FriendRequest.class)))
                .willThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate entry"));

        // when & then
        assertThatThrownBy(() -> sendFriendRequestService.sendFriendRequest(requesterId, receiverId))
                .isInstanceOf(InvalidFriendRequestException.class)
                .hasMessageContaining("이미 친구 요청을 보냈습니다");
    }

    @Test
    @DisplayName("푸시 알림 실패 시에도 친구 요청은 성공")
    void should_succeedEvenWhenPushNotificationFails() {
        // given
        Long requesterId = 1L;
        Long receiverId = 2L;
        Long requestId = 100L;

        User requester = User.builder()
                .id(requesterId)
                .email(new Email("requester@example.com"))
                .nickname("요청자닉네임")
                .passwordHash("hash")
                .build();

        doNothing().when(userValidator).validateNotSelfAction(requesterId, receiverId, "친구 요청");
        given(userValidator.validateUserExists(receiverId)).willReturn(
                User.builder().id(receiverId).email(new Email("test@example.com")).nickname("test").passwordHash("hash").build()
        );
        given(userValidator.validateUserExists(requesterId)).willReturn(requester);
        given(friendRepository.existsByUserIdAndFriendId(requesterId, receiverId)).willReturn(false);
        given(friendRequestRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId)).willReturn(false);
        given(friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(
                receiverId, requesterId, FriendRequest.RequestStatus.PENDING)).willReturn(false);
        given(idGenerator.nextId()).willReturn(requestId);
        given(friendRequestRepository.save(any(FriendRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException("Push notification failed"))
                .when(notificationCommandPort).sendFriendRequestNotification(anyLong(), anyString());

        // when
        Long result = sendFriendRequestService.sendFriendRequest(requesterId, receiverId);

        // then - 푸시 알림 실패해도 친구 요청은 성공
        assertThat(result).isEqualTo(requestId);
        verify(friendRequestRepository).save(any(FriendRequest.class));
    }
}
