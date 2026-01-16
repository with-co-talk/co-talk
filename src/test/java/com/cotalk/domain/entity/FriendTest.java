package com.cotalk.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Friend 엔티티")
class FriendTest {

    @Nested
    @DisplayName("생성 시")
    class Creation {

        @Test
        @DisplayName("사용자 ID와 친구 ID로 친구 관계를 생성할 수 있다")
        void should_CreateFriend_when_ValidInputsProvided() {
            // given
            Long userId = 1L;
            Long friendId = 2L;

            // when
            Friend friend = Friend.builder()
                    .userId(userId)
                    .friendId(friendId)
                    .status(Friend.FriendStatus.ACCEPTED)
                    .build();

            // then
            assertThat(friend.getUserId()).isEqualTo(userId);
            assertThat(friend.getFriendId()).isEqualTo(friendId);
            assertThat(friend.getStatus()).isEqualTo(Friend.FriendStatus.ACCEPTED);
        }
    }
}
