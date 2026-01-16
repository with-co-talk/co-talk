package com.cotalk.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FriendRequest 엔티티")
class FriendRequestTest {

    @Nested
    @DisplayName("생성 시")
    class Creation {

        @Test
        @DisplayName("요청자 ID와 수신자 ID로 친구 요청을 생성할 수 있다")
        void should_CreateFriendRequest_when_ValidInputsProvided() {
            // given
            Long requesterId = 1L;
            Long receiverId = 2L;

            // when
            FriendRequest request = FriendRequest.builder()
                    .requesterId(requesterId)
                    .receiverId(receiverId)
                    .status(FriendRequest.RequestStatus.PENDING)
                    .build();

            // then
            assertThat(request.getRequesterId()).isEqualTo(requesterId);
            assertThat(request.getReceiverId()).isEqualTo(receiverId);
            assertThat(request.getStatus()).isEqualTo(FriendRequest.RequestStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("상태 확인 시")
    class StatusCheck {

        @Test
        @DisplayName("PENDING 상태면 isPending이 true를 반환한다")
        void should_ReturnTrue_when_StatusIsPending() {
            // given
            FriendRequest request = FriendRequest.builder()
                    .requesterId(1L)
                    .receiverId(2L)
                    .status(FriendRequest.RequestStatus.PENDING)
                    .build();

            // then
            assertThat(request.isPending()).isTrue();
        }

        @Test
        @DisplayName("ACCEPTED 상태면 isPending이 false를 반환한다")
        void should_ReturnFalse_when_StatusIsAccepted() {
            // given
            FriendRequest request = FriendRequest.builder()
                    .requesterId(1L)
                    .receiverId(2L)
                    .status(FriendRequest.RequestStatus.ACCEPTED)
                    .build();

            // then
            assertThat(request.isPending()).isFalse();
        }
    }

    @Nested
    @DisplayName("수락 시")
    class Accept {

        @Test
        @DisplayName("PENDING 상태에서 수락하면 ACCEPTED 상태가 된다")
        void should_ChangeToAccepted_when_AcceptedFromPending() {
            // given
            FriendRequest request = FriendRequest.builder()
                    .requesterId(1L)
                    .receiverId(2L)
                    .status(FriendRequest.RequestStatus.PENDING)
                    .build();

            // when
            request.accept();

            // then
            assertThat(request.getStatus()).isEqualTo(FriendRequest.RequestStatus.ACCEPTED);
        }

        @Test
        @DisplayName("PENDING이 아닌 상태에서 수락하면 예외가 발생한다")
        void should_ThrowException_when_AcceptedFromNonPending() {
            // given
            FriendRequest request = FriendRequest.builder()
                    .requesterId(1L)
                    .receiverId(2L)
                    .status(FriendRequest.RequestStatus.ACCEPTED)
                    .build();

            // when & then
            assertThatThrownBy(() -> request.accept())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("거절 시")
    class Reject {

        @Test
        @DisplayName("PENDING 상태에서 거절하면 REJECTED 상태가 된다")
        void should_ChangeToRejected_when_RejectedFromPending() {
            // given
            FriendRequest request = FriendRequest.builder()
                    .requesterId(1L)
                    .receiverId(2L)
                    .status(FriendRequest.RequestStatus.PENDING)
                    .build();

            // when
            request.reject();

            // then
            assertThat(request.getStatus()).isEqualTo(FriendRequest.RequestStatus.REJECTED);
        }

        @Test
        @DisplayName("PENDING이 아닌 상태에서 거절하면 예외가 발생한다")
        void should_ThrowException_when_RejectedFromNonPending() {
            // given
            FriendRequest request = FriendRequest.builder()
                    .requesterId(1L)
                    .receiverId(2L)
                    .status(FriendRequest.RequestStatus.REJECTED)
                    .build();

            // when & then
            assertThatThrownBy(() -> request.reject())
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
