package com.cotalk.adapter.inbound.rest.dto.chatroom;

import com.cotalk.domain.entity.ChatRoom.ChatRoomType;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.ChatRoomMember.MemberRole;
import com.cotalk.domain.entity.ChatRoomSummary;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChatRoom DTO 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("ChatRoom DTO")
class ChatRoomDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("CreateChatRoomRequest")
    class CreateChatRoomRequestTest {

        @Test
        @DisplayName("of 메서드로 인스턴스를 생성한다")
        void should_createInstance_when_ofMethodCalled() {
            // when
            CreateChatRoomRequest request = CreateChatRoomRequest.of(1L, 2L);

            // then
            assertThat(request.userId1()).isEqualTo(1L);
            assertThat(request.userId2()).isEqualTo(2L);
        }

        @Test
        @DisplayName("userId1이 null이면 유효성 검사 실패")
        void should_failValidation_when_userId1IsNull() {
            // given
            CreateChatRoomRequest request = new CreateChatRoomRequest(null, 2L);

            // when
            Set<ConstraintViolation<CreateChatRoomRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("첫 번째 사용자 ID");
        }

        @Test
        @DisplayName("userId2가 null이면 유효성 검사 실패")
        void should_failValidation_when_userId2IsNull() {
            // given
            CreateChatRoomRequest request = new CreateChatRoomRequest(1L, null);

            // when
            Set<ConstraintViolation<CreateChatRoomRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("두 번째 사용자 ID");
        }
    }

    @Nested
    @DisplayName("CreateGroupChatRoomRequest")
    class CreateGroupChatRoomRequestTest {

        @Test
        @DisplayName("of 메서드로 인스턴스를 생성한다")
        void should_createInstance_when_ofMethodCalled() {
            // when
            CreateGroupChatRoomRequest request = CreateGroupChatRoomRequest.of(
                    1L, "테스트 그룹", List.of(2L, 3L));

            // then
            assertThat(request.creatorId()).isEqualTo(1L);
            assertThat(request.roomName()).isEqualTo("테스트 그룹");
            assertThat(request.memberIds()).containsExactly(2L, 3L);
        }

        @Test
        @DisplayName("creatorId가 null이면 유효성 검사 실패")
        void should_failValidation_when_creatorIdIsNull() {
            // given
            CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest(null, "그룹", List.of(1L));

            // when
            Set<ConstraintViolation<CreateGroupChatRoomRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
        }

        @Test
        @DisplayName("roomName이 비어있으면 유효성 검사 실패")
        void should_failValidation_when_roomNameIsBlank() {
            // given
            CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest(1L, "", List.of(1L));

            // when
            Set<ConstraintViolation<CreateGroupChatRoomRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("roomName이 50자 초과하면 유효성 검사 실패")
        void should_failValidation_when_roomNameExceeds50Chars() {
            // given
            String longName = "a".repeat(51);
            CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest(1L, longName, List.of(1L));

            // when
            Set<ConstraintViolation<CreateGroupChatRoomRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("memberIds가 null이면 유효성 검사 실패")
        void should_failValidation_when_memberIdsIsNull() {
            // given
            CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest(1L, "그룹", null);

            // when
            Set<ConstraintViolation<CreateGroupChatRoomRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
        }
    }

    @Nested
    @DisplayName("InviteMembersRequest")
    class InviteMembersRequestTest {

        @Test
        @DisplayName("of 메서드로 인스턴스를 생성한다")
        void should_createInstance_when_ofMethodCalled() {
            // when
            InviteMembersRequest request = InviteMembersRequest.of(1L, List.of(2L, 3L));

            // then
            assertThat(request.inviterId()).isEqualTo(1L);
            assertThat(request.inviteeIds()).containsExactly(2L, 3L);
        }

        @Test
        @DisplayName("inviterId가 null이면 유효성 검사 실패")
        void should_failValidation_when_inviterIdIsNull() {
            // given
            InviteMembersRequest request = new InviteMembersRequest(null, List.of(1L));

            // when
            Set<ConstraintViolation<InviteMembersRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
        }

        @Test
        @DisplayName("inviteeIds가 null이면 유효성 검사 실패")
        void should_failValidation_when_inviteeIdsIsNull() {
            // given
            InviteMembersRequest request = new InviteMembersRequest(1L, null);

            // when
            Set<ConstraintViolation<InviteMembersRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
        }
    }

    @Nested
    @DisplayName("UpdateChatRoomNameRequest")
    class UpdateChatRoomNameRequestTest {

        @Test
        @DisplayName("of 메서드로 인스턴스를 생성한다")
        void should_createInstance_when_ofMethodCalled() {
            // when
            UpdateChatRoomNameRequest request = UpdateChatRoomNameRequest.of(1L, "새 이름");

            // then
            assertThat(request.userId()).isEqualTo(1L);
            assertThat(request.newName()).isEqualTo("새 이름");
        }

        @Test
        @DisplayName("userId가 null이면 유효성 검사 실패")
        void should_failValidation_when_userIdIsNull() {
            // given
            UpdateChatRoomNameRequest request = new UpdateChatRoomNameRequest(null, "이름");

            // when
            Set<ConstraintViolation<UpdateChatRoomNameRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
        }

        @Test
        @DisplayName("newName이 비어있으면 유효성 검사 실패")
        void should_failValidation_when_newNameIsBlank() {
            // given
            UpdateChatRoomNameRequest request = new UpdateChatRoomNameRequest(1L, "");

            // when
            Set<ConstraintViolation<UpdateChatRoomNameRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("newName이 50자 초과하면 유효성 검사 실패")
        void should_failValidation_when_newNameExceeds50Chars() {
            // given
            String longName = "a".repeat(51);
            UpdateChatRoomNameRequest request = new UpdateChatRoomNameRequest(1L, longName);

            // when
            Set<ConstraintViolation<UpdateChatRoomNameRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("SetAnnouncementRequest")
    class SetAnnouncementRequestTest {

        @Test
        @DisplayName("of 메서드로 인스턴스를 생성한다")
        void should_createInstance_when_ofMethodCalled() {
            // when
            SetAnnouncementRequest request = SetAnnouncementRequest.of(1L, "공지사항 내용");

            // then
            assertThat(request.userId()).isEqualTo(1L);
            assertThat(request.announcement()).isEqualTo("공지사항 내용");
        }

        @Test
        @DisplayName("userId가 null이면 유효성 검사 실패")
        void should_failValidation_when_userIdIsNull() {
            // given
            SetAnnouncementRequest request = new SetAnnouncementRequest(null, "공지");

            // when
            Set<ConstraintViolation<SetAnnouncementRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
        }

        @Test
        @DisplayName("announcement가 비어있으면 유효성 검사 실패")
        void should_failValidation_when_announcementIsBlank() {
            // given
            SetAnnouncementRequest request = new SetAnnouncementRequest(1L, "");

            // when
            Set<ConstraintViolation<SetAnnouncementRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("announcement가 500자 초과하면 유효성 검사 실패")
        void should_failValidation_when_announcementExceeds500Chars() {
            // given
            String longAnnouncement = "a".repeat(501);
            SetAnnouncementRequest request = new SetAnnouncementRequest(1L, longAnnouncement);

            // when
            Set<ConstraintViolation<SetAnnouncementRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("ChatRoomDto")
    class ChatRoomDtoFromTest {

        @Test
        @DisplayName("ChatRoomSummary로부터 DTO를 생성한다")
        void should_createDto_when_fromSummaryCalled() {
            // given
            LocalDateTime now = LocalDateTime.now();
            ChatRoomSummary summary = new ChatRoomSummary(
                    1L, "채팅방 이름", ChatRoomType.DIRECT, now,
                    "마지막 메시지", "TEXT", now, 5L,
                    2L, "상대방닉네임", "https://avatar.url", false, true, now
            );

            // when
            ChatRoomDto dto = ChatRoomDto.from(summary);

            // then
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("채팅방 이름");
            assertThat(dto.type()).isEqualTo("DIRECT");
            assertThat(dto.createdAt()).isEqualTo(now);
            assertThat(dto.lastMessage()).isEqualTo("마지막 메시지");
            assertThat(dto.lastMessageAt()).isEqualTo(now);
            assertThat(dto.unreadCount()).isEqualTo(5L);
            assertThat(dto.otherUserId()).isEqualTo(2L);
            assertThat(dto.otherUserNickname()).isEqualTo("상대방닉네임");
            assertThat(dto.otherUserAvatarUrl()).isEqualTo("https://avatar.url");
        }

        @Test
        @DisplayName("그룹 채팅방 Summary로부터 DTO를 생성한다")
        void should_createDto_when_groupChatSummary() {
            // given
            LocalDateTime now = LocalDateTime.now();
            ChatRoomSummary summary = new ChatRoomSummary(
                    1L, "그룹 채팅방", ChatRoomType.GROUP, now,
                    null, null, null, 0L,
                    null, null, null, false, false, null
            );

            // when
            ChatRoomDto dto = ChatRoomDto.from(summary);

            // then
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.type()).isEqualTo("GROUP");
            assertThat(dto.otherUserId()).isNull();
            assertThat(dto.otherUserNickname()).isNull();
        }
    }

    @Nested
    @DisplayName("Response DTOs")
    class ResponseDtosTest {

        @Test
        @DisplayName("CreateChatRoomResponse of 메서드로 생성")
        void should_createChatRoomResponse_when_ofCalled() {
            // when
            CreateChatRoomResponse response = CreateChatRoomResponse.of(1L, "생성 완료");

            // then
            assertThat(response.roomId()).isEqualTo(1L);
            assertThat(response.message()).isEqualTo("생성 완료");
        }

        @Test
        @DisplayName("UpdateChatRoomNameResponse of 메서드로 생성")
        void should_createUpdateNameResponse_when_ofCalled() {
            // when
            UpdateChatRoomNameResponse response = UpdateChatRoomNameResponse.of("새이름", "변경 완료");

            // then
            assertThat(response.name()).isEqualTo("새이름");
            assertThat(response.message()).isEqualTo("변경 완료");
        }

        @Test
        @DisplayName("AnnouncementResponse of 메서드로 생성")
        void should_createAnnouncementResponse_when_ofCalled() {
            // when
            AnnouncementResponse response = AnnouncementResponse.of("공지 내용", "설정 완료");

            // then
            assertThat(response.announcement()).isEqualTo("공지 내용");
            assertThat(response.message()).isEqualTo("설정 완료");
        }

        @Test
        @DisplayName("ChatRoomsResponse of 메서드로 생성")
        void should_createChatRoomsResponse_when_ofCalled() {
            // given
            LocalDateTime now = LocalDateTime.now();
            List<ChatRoomDto> rooms = List.of(
                    new ChatRoomDto(1L, "방1", "PRIVATE", now, null, null, null, 0, null, null, null, false, false, null),
                    new ChatRoomDto(2L, "방2", "GROUP", now, null, null, null, 0, null, null, null, false, false, null)
            );

            // when
            ChatRoomsResponse response = ChatRoomsResponse.of(rooms);

            // then
            assertThat(response.rooms()).hasSize(2);
        }

        @Test
        @DisplayName("AdminResponse of 메서드로 생성")
        void should_createAdminResponse_when_ofCalled() {
            // when
            AdminResponse response = AdminResponse.of(1L, "ADMIN", "관리자 설정 완료");

            // then
            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.role()).isEqualTo("ADMIN");
            assertThat(response.message()).isEqualTo("관리자 설정 완료");
        }

        @Test
        @DisplayName("AdminResponse from ChatRoomMember 메서드로 생성")
        void should_createAdminResponse_when_fromMemberCalled() {
            // given
            ChatRoomMember member = ChatRoomMember.builder()
                    .userId(1L)
                    .chatRoomId(100L)
                    .role(MemberRole.ADMIN)
                    .build();

            // when
            AdminResponse response = AdminResponse.from(member, "관리자로 변경됨");

            // then
            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.role()).isEqualTo("ADMIN");
            assertThat(response.message()).isEqualTo("관리자로 변경됨");
        }
    }
}
