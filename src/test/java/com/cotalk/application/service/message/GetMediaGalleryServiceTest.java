package com.cotalk.application.service.message;

import com.cotalk.common.fixture.MessageTestFixture;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.inbound.message.GetMediaGalleryUseCase;
import com.cotalk.domain.port.outbound.FileStorage;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.cotalk.domain.model.PageQuery;


import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * GetMediaGalleryService 단위 테스트.
 * 미디어 갤러리 조회 로직을 검증한다.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
class GetMediaGalleryServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomMemberValidator chatRoomMemberValidator;

    @Mock
    private FileStorage fileStorage;

    private GetMediaGalleryService getMediaGalleryService;

    private static final int PRESIGN_EXPIRY_MINUTES = 10;

    @BeforeEach
    void setUp() {
        getMediaGalleryService = new GetMediaGalleryService(
                messageRepository, userRepository, chatRoomMemberValidator,
                fileStorage, PRESIGN_EXPIRY_MINUTES);
        // 기본 동작: 첨부파일 URL은 그대로 통과시켜 기존 검증을 유지한다.
        // 실제 운영에서는 멤버십 검증 후 단기 Pre-signed URL로 재발급된다(H-1).
        lenient().when(fileStorage.presignAttachmentUrl(anyString(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void should_returnPhotoGallery_when_typeIsPhoto() {
        // given
        Long chatRoomId = 10L;
        Long userId = 1L;
        String type = "PHOTO";
        int page = 0;
        int size = 20;

        Message image1 = MessageTestFixture.createImageMessage(101L, chatRoomId, userId, "http://example.com/img1.jpg");
        Message image2 = MessageTestFixture.createImageMessage(102L, chatRoomId, 2L, "http://example.com/img2.jpg");
        List<Message> images = List.of(image1, image2);

        User user1 = User.builder()
                .id(userId)
                .nickname("사용자1")
                .build();

        User user2 = User.builder()
                .id(2L)
                .nickname("사용자2")
                .build();

        PageQuery query = PageQuery.of(page, size);

        given(messageRepository.findByTypeInChatRoom(eq(chatRoomId), eq(List.of(Message.MessageType.IMAGE)), eq(query)))
                .willReturn(images);
        given(userRepository.findAllById(any())).willReturn(List.of(user1, user2));

        // when
        GetMediaGalleryUseCase.MediaGalleryResult result = getMediaGalleryService.getMediaGallery(chatRoomId, userId, type, page, size);

        // then
        verify(chatRoomMemberValidator).validateMembership(chatRoomId, userId);

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).messageId()).isEqualTo(101L);
        assertThat(result.items().get(0).type()).isEqualTo("IMAGE");
        assertThat(result.items().get(0).fileUrl()).isEqualTo("http://example.com/img1.jpg");
        assertThat(result.items().get(0).senderNickname()).isEqualTo("사용자1");

        assertThat(result.items().get(1).messageId()).isEqualTo(102L);
        assertThat(result.items().get(1).senderNickname()).isEqualTo("사용자2");

        assertThat(result.nextCursor()).isEqualTo(102L);
        assertThat(result.hasMore()).isFalse(); // 2 < size(20) 이므로 마지막 페이지
    }

    @Test
    void should_returnFileGallery_when_typeIsFile() {
        // given
        Long chatRoomId = 10L;
        Long userId = 1L;
        String type = "FILE";
        int page = 0;
        int size = 20;

        Message file1 = MessageTestFixture.createFileMessage(201L, chatRoomId, userId, "http://example.com/file1.pdf", "document.pdf");
        Message file2 = MessageTestFixture.createFileMessage(202L, chatRoomId, 2L, "http://example.com/file2.pdf", "report.pdf");
        List<Message> files = List.of(file1, file2);

        User user1 = User.builder().id(userId).nickname("사용자1").build();
        User user2 = User.builder().id(2L).nickname("사용자2").build();

        PageQuery query = PageQuery.of(page, size);

        given(messageRepository.findByTypeInChatRoom(eq(chatRoomId), eq(List.of(Message.MessageType.FILE)), eq(query)))
                .willReturn(files);
        given(userRepository.findAllById(any())).willReturn(List.of(user1, user2));

        // when
        GetMediaGalleryUseCase.MediaGalleryResult result = getMediaGalleryService.getMediaGallery(chatRoomId, userId, type, page, size);

        // then
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).type()).isEqualTo("FILE");
        assertThat(result.items().get(0).fileName()).isEqualTo("document.pdf");
        assertThat(result.items().get(1).fileName()).isEqualTo("report.pdf");
    }

    @Test
    void should_returnLinkGallery_when_typeIsLink() {
        // given
        Long chatRoomId = 10L;
        Long userId = 1L;
        String type = "LINK";
        int page = 0;
        int size = 20;

        Message linkMessage = MessageTestFixture.createMessage(301L, chatRoomId, userId, "https://example.com");
        linkMessage.applyLinkPreview(
                "https://example.com",
                "Example Site",
                "This is an example",
                "http://example.com/og.jpg"
        );

        List<Message> links = List.of(linkMessage);

        User user1 = User.builder().id(userId).nickname("사용자1").build();

        PageQuery query = PageQuery.of(page, size);

        given(messageRepository.findMessagesWithLinkPreview(eq(chatRoomId), eq(query))).willReturn(links);
        given(userRepository.findAllById(any())).willReturn(List.of(user1));

        // when
        GetMediaGalleryUseCase.MediaGalleryResult result = getMediaGalleryService.getMediaGallery(chatRoomId, userId, type, page, size);

        // then
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).linkPreviewUrl()).isEqualTo("https://example.com");
        assertThat(result.items().get(0).linkPreviewTitle()).isEqualTo("Example Site");
        assertThat(result.items().get(0).linkPreviewDescription()).isEqualTo("This is an example");
        assertThat(result.items().get(0).linkPreviewImageUrl()).isEqualTo("http://example.com/og.jpg");
    }

    @Test
    void should_returnEmptyGallery_when_noMessages() {
        // given
        Long chatRoomId = 10L;
        Long userId = 1L;
        String type = "PHOTO";
        int page = 0;
        int size = 20;

        PageQuery query = PageQuery.of(page, size);

        given(messageRepository.findByTypeInChatRoom(eq(chatRoomId), any(), eq(query))).willReturn(List.of());
        given(userRepository.findAllById(any())).willReturn(List.of());

        // when
        GetMediaGalleryUseCase.MediaGalleryResult result = getMediaGalleryService.getMediaGallery(chatRoomId, userId, type, page, size);

        // then
        assertThat(result.items()).isEmpty();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.hasMore()).isFalse();
    }

    @Test
    void should_throwException_when_userIsNotMember() {
        // given
        Long chatRoomId = 10L;
        Long userId = 1L;
        String type = "PHOTO";

        doThrow(new ChatRoomAccessDeniedException("채팅방 멤버가 아닙니다."))
                .when(chatRoomMemberValidator).validateMembership(chatRoomId, userId);

        // when & then
        assertThatThrownBy(() -> getMediaGalleryService.getMediaGallery(chatRoomId, userId, type, 0, 20))
                .isInstanceOf(ChatRoomAccessDeniedException.class)
                .hasMessage("채팅방 멤버가 아닙니다.");

        // 멤버십 검증 실패 시 Pre-signed URL을 발급하지 않는다(H-1).
        verify(fileStorage, never()).presignAttachmentUrl(anyString(), anyInt());
    }

    @Test
    @DisplayName("멤버 조회 시 첨부파일은 영구 공개 URL이 아닌 단기 Pre-signed URL로 재발급된다")
    void should_returnPresignedAttachmentUrl_when_memberFetchesGallery() {
        // given
        Long chatRoomId = 10L;
        Long userId = 1L;
        String type = "PHOTO";
        int page = 0;
        int size = 20;

        String storedUrl = "http://minio.example.com/cotalk/uploads/1/abc.png";
        Message image = MessageTestFixture.createImageMessage(101L, chatRoomId, userId, storedUrl);
        User user = User.builder().id(userId).nickname("사용자1").build();
        PageQuery query = PageQuery.of(page, size);

        given(messageRepository.findByTypeInChatRoom(eq(chatRoomId), eq(List.of(Message.MessageType.IMAGE)), eq(query)))
                .willReturn(List.of(image));
        given(userRepository.findAllById(any())).willReturn(List.of(user));
        given(fileStorage.presignAttachmentUrl(eq(storedUrl), eq(PRESIGN_EXPIRY_MINUTES)))
                .willReturn(storedUrl + "?X-Amz-Signature=signed");

        // when
        GetMediaGalleryUseCase.MediaGalleryResult result =
                getMediaGalleryService.getMediaGallery(chatRoomId, userId, type, page, size);

        // then
        verify(chatRoomMemberValidator).validateMembership(chatRoomId, userId);
        assertThat(result.items().get(0).fileUrl()).contains("X-Amz-Signature");
        assertThat(result.items().get(0).fileUrl()).isNotEqualTo(storedUrl);
        verify(fileStorage).presignAttachmentUrl(storedUrl, PRESIGN_EXPIRY_MINUTES);
    }

    @Test
    void should_throwException_when_unsupportedMediaType() {
        // given
        Long chatRoomId = 10L;
        Long userId = 1L;
        String type = "UNSUPPORTED";

        // when & then
        assertThatThrownBy(() -> getMediaGalleryService.getMediaGallery(chatRoomId, userId, type, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 미디어 유형입니다");
    }

    @Test
    void should_returnHasMoreFalse_when_lastPage() {
        // given
        Long chatRoomId = 10L;
        Long userId = 1L;
        String type = "PHOTO";
        int page = 0;
        int size = 20;

        // size보다 적은 수의 결과 반환 (마지막 페이지)
        Message image = MessageTestFixture.createImageMessage(101L, chatRoomId, userId, "http://example.com/img.jpg");
        List<Message> images = List.of(image);

        User user = User.builder().id(userId).nickname("사용자1").build();

        PageQuery query = PageQuery.of(page, size);

        given(messageRepository.findByTypeInChatRoom(eq(chatRoomId), any(), eq(query))).willReturn(images);
        given(userRepository.findAllById(any())).willReturn(List.of(user));

        // when
        GetMediaGalleryUseCase.MediaGalleryResult result = getMediaGalleryService.getMediaGallery(chatRoomId, userId, type, page, size);

        // then
        assertThat(result.items()).hasSize(1);
        assertThat(result.hasMore()).isFalse(); // size(20)보다 적으므로 더 이상 없음
    }
}
