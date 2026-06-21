package com.cotalk.application.service.user;

import com.cotalk.adapter.outbound.persistence.auth.EmailVerificationTokenRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.auth.PasswordResetTokenRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.auth.TermsAgreementRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.chatroom.ChatRoomMemberRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.chatroom.ChatRoomRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.friend.BlockRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.friend.FriendRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.friend.FriendRequestRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.friend.HiddenFriendRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.mapper.BlockMapper;
import com.cotalk.adapter.outbound.persistence.mapper.DeviceTokenMapper;
import com.cotalk.adapter.outbound.persistence.mapper.EmailVerificationTokenMapper;
import com.cotalk.adapter.outbound.persistence.mapper.FriendMapper;
import com.cotalk.adapter.outbound.persistence.mapper.FriendRequestMapper;
import com.cotalk.adapter.outbound.persistence.mapper.HiddenFriendMapper;
import com.cotalk.adapter.outbound.persistence.mapper.NotificationSettingMapper;
import com.cotalk.adapter.outbound.persistence.mapper.PasswordResetTokenMapper;
import com.cotalk.adapter.outbound.persistence.mapper.ProfileHistoryMapper;
import com.cotalk.adapter.outbound.persistence.mapper.RefreshTokenMapper;
import com.cotalk.adapter.outbound.persistence.mapper.ReportMapper;
import com.cotalk.adapter.outbound.persistence.mapper.TermsAgreementMapper;
import com.cotalk.adapter.outbound.persistence.mapper.UserMapper;
import com.cotalk.adapter.outbound.persistence.message.MessageReactionRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.message.MessageRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.notification.DeviceTokenRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.notification.NotificationSettingRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.profile.ProfileHistoryRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.refreshtoken.RefreshTokenRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.report.ReportRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.user.UserRepositoryAdapter;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoom.ChatRoomType;
import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.HiddenFriend;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.Message.MessageType;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.entity.RefreshToken;
import com.cotalk.domain.entity.Report;
import com.cotalk.domain.entity.Report.ReportReason;
import com.cotalk.domain.entity.Report.ReportStatus;
import com.cotalk.domain.entity.Report.ReportType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.port.outbound.HiddenFriendRepository;
import com.cotalk.domain.port.outbound.MessageReactionRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.PasswordEncoderPort;
import com.cotalk.domain.port.outbound.RefreshTokenRepository;
import com.cotalk.domain.port.outbound.ReportRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.config.JpaAuditingConfig;
import com.cotalk.infrastructure.security.SpringPasswordEncoderAdapter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 회원 탈퇴 FK 제약 회귀 통합 테스트.
 *
 * <p>실제 운영 스키마(V1__init_schema.sql)에 존재하는 외래키 제약을 H2에 그대로 적용한 뒤
 * ({@code sql/delete-account-fk-constraints.sql}), 메시지/반응/피신고/피숨김(friend_id) 데이터를
 * 가진 실사용자가 탈퇴할 때 FK 위반 없이 탈퇴되고 관련 레코드가 정리되는지 검증한다.</p>
 *
 * <p>엔티티에는 {@code @ManyToOne}/{@code @ForeignKey} 매핑이 없어 {@code ddl-auto: create-drop}만으로는
 * FK 제약이 생성되지 않으므로, 순수 Mockito 테스트로는 이 결함을 잡을 수 없다. 이 테스트는
 * 실제 DB FK 제약을 태워 회귀를 방지한다.</p>
 *
 * @author seunggu.lee
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({
        DeleteAccountServiceFkIntegrationTest.TestBeans.class,
        UserRepositoryAdapter.class,
        MessageRepositoryAdapter.class,
        MessageReactionRepositoryAdapter.class,
        ChatRoomRepositoryAdapter.class,
        ChatRoomMemberRepositoryAdapter.class,
        FriendRepositoryAdapter.class,
        FriendRequestRepositoryAdapter.class,
        BlockRepositoryAdapter.class,
        ReportRepositoryAdapter.class,
        HiddenFriendRepositoryAdapter.class,
        DeviceTokenRepositoryAdapter.class,
        NotificationSettingRepositoryAdapter.class,
        PasswordResetTokenRepositoryAdapter.class,
        EmailVerificationTokenRepositoryAdapter.class,
        TermsAgreementRepositoryAdapter.class,
        RefreshTokenRepositoryAdapter.class,
        ProfileHistoryRepositoryAdapter.class,
        UserMapper.class,
        RefreshTokenMapper.class,
        PasswordResetTokenMapper.class,
        EmailVerificationTokenMapper.class,
        TermsAgreementMapper.class,
        DeviceTokenMapper.class,
        NotificationSettingMapper.class,
        ProfileHistoryMapper.class,
        ReportMapper.class,
        FriendMapper.class,
        FriendRequestMapper.class,
        BlockMapper.class,
        HiddenFriendMapper.class,
        JpaAuditingConfig.class
})
@Sql(scripts = "/sql/delete-account-fk-constraints.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("DeleteAccountService FK 제약 통합 테스트")
class DeleteAccountServiceFkIntegrationTest {

    private static final String RAW_PASSWORD = "password123";

    @Autowired
    private DeleteAccountService deleteAccountService;

    @Autowired
    private PasswordEncoderPort passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageReactionRepository messageReactionRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private HiddenFriendRepository hiddenFriendRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenRepositoryAdapter refreshTokenRepositoryAdapter;

    @Autowired
    private UserRepositoryAdapter userRepositoryAdapter;

    @Autowired
    private ChatRoomRepositoryAdapter chatRoomRepository;

    @Autowired
    private MessageRepositoryAdapter messageRepositoryAdapter;

    @Autowired
    private MessageReactionRepositoryAdapter messageReactionRepositoryAdapter;

    @Autowired
    private ReportRepositoryAdapter reportRepositoryAdapter;

    @Autowired
    private HiddenFriendRepositoryAdapter hiddenFriendRepositoryAdapter;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 탈퇴 대상 사용자. 메시지를 보낸 적이 있는 실사용자.
     */
    private User leavingUser;

    /**
     * 탈퇴 대상이 아닌 다른 사용자.
     */
    private User otherUser;

    private Long leavingMessageId;

    @BeforeEach
    void setUp() {
        leavingUser = userRepositoryAdapter.save(User.builder()
                .id(1L)
                .email(new Email("leaving@example.com"))
                .passwordHash(passwordEncoder.encode(RAW_PASSWORD))
                .nickname("탈퇴유저")
                .build());

        otherUser = userRepositoryAdapter.save(User.builder()
                .id(2L)
                .email(new Email("other@example.com"))
                .passwordHash("hash")
                .nickname("상대유저")
                .build());

        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder()
                .id(100L)
                .type(ChatRoomType.DIRECT)
                .build());

        // 탈퇴 대상 사용자가 보낸 메시지 (messages.sender_id)
        Message leavingMessage = messageRepositoryAdapter.save(Message.builder()
                .id(1000L)
                .chatRoomId(chatRoom.getId())
                .senderId(leavingUser.getId())
                .content("탈퇴 유저가 보낸 메시지")
                .type(MessageType.TEXT)
                .build());
        leavingMessageId = leavingMessage.getId();

        // 상대 사용자가 보낸 메시지 (탈퇴 후에도 남아 있어야 함)
        Message otherMessage = messageRepositoryAdapter.save(Message.builder()
                .id(1001L)
                .chatRoomId(chatRoom.getId())
                .senderId(otherUser.getId())
                .content("상대가 보낸 메시지")
                .type(MessageType.TEXT)
                .build());

        // 탈퇴 대상 사용자가 남긴 반응 (message_reactions.user_id) - 상대 메시지에 반응
        messageReactionRepositoryAdapter.save(MessageReaction.builder()
                .messageId(otherMessage.getId())
                .userId(leavingUser.getId())
                .emoji(Emoji.HEART)
                .build());

        // 상대 사용자가 탈퇴 대상의 메시지에 남긴 반응 (message_reactions.message_id -> leavingMessage)
        messageReactionRepositoryAdapter.save(MessageReaction.builder()
                .messageId(leavingMessage.getId())
                .userId(otherUser.getId())
                .emoji(Emoji.THUMBS_UP)
                .build());

        // 상대 사용자가 탈퇴 대상 사용자를 신고 (reports.reported_user_id)
        reportRepositoryAdapter.save(Report.builder()
                .id(2000L)
                .reporterId(otherUser.getId())
                .reportedUserId(leavingUser.getId())
                .type(ReportType.USER)
                .reason(ReportReason.SPAM)
                .status(ReportStatus.PENDING)
                .build());

        // 상대 사용자가 탈퇴 대상의 메시지를 신고 (reports.reported_message_id -> leavingMessage)
        reportRepositoryAdapter.save(Report.builder()
                .id(2001L)
                .reporterId(otherUser.getId())
                .reportedMessageId(leavingMessage.getId())
                .type(ReportType.MESSAGE)
                .reason(ReportReason.SPAM)
                .status(ReportStatus.PENDING)
                .build());

        // 탈퇴 대상 사용자가 한 신고 (reports.reporter_id)
        reportRepositoryAdapter.save(Report.builder()
                .id(2002L)
                .reporterId(leavingUser.getId())
                .reportedUserId(otherUser.getId())
                .type(ReportType.USER)
                .reason(ReportReason.SPAM)
                .status(ReportStatus.PENDING)
                .build());

        // 상대 사용자가 탈퇴 대상 사용자를 숨김 (hidden_friends.friend_id -> leavingUser)
        hiddenFriendRepositoryAdapter.save(HiddenFriend.builder()
                .userId(otherUser.getId())
                .friendId(leavingUser.getId())
                .build());

        // 탈퇴 대상 사용자가 상대를 숨김 (hidden_friends.user_id -> leavingUser)
        hiddenFriendRepositoryAdapter.save(HiddenFriend.builder()
                .userId(leavingUser.getId())
                .friendId(otherUser.getId())
                .build());

        // 탈퇴 대상 사용자의 Refresh Token (refresh_tokens.user_id -> leavingUser)
        // revoke(플래그)만으로는 fk_refresh_tokens_user 제약 때문에 탈퇴 시 FK 위반이 발생하므로,
        // 탈퇴 경로가 행을 실제 삭제하는지 검증하기 위한 픽스처.
        refreshTokenRepositoryAdapter.save(RefreshToken.builder()
                .id(3000L)
                .userId(leavingUser.getId())
                .token("leaving-user-refresh-token")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build());

        // 영속성 컨텍스트를 비워 이후 조회가 실제 DB 상태를 반영하도록 한다.
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("메시지/반응/피신고/피숨김이 있는 실사용자도 FK 위반 없이 탈퇴되고 관련 레코드가 정리된다")
    void should_deleteAccount_when_userHasMessagesReactionsReportsAndHiddenByOthers() {
        // when & then: FK 위반(롤백) 없이 탈퇴 성공해야 한다
        assertThatCode(() -> deleteAccountService.deleteAccount(leavingUser.getId(), RAW_PASSWORD))
                .doesNotThrowAnyException();

        entityManager.flush();
        entityManager.clear();

        // 사용자 삭제됨
        assertThat(userRepository.findById(leavingUser.getId())).isEmpty();
        // 상대 사용자는 유지
        assertThat(userRepository.findById(otherUser.getId())).isPresent();

        // 탈퇴 사용자가 보낸 메시지 삭제됨
        assertThat(messageRepository.findById(leavingMessageId)).isEmpty();

        // 탈퇴 사용자가 남긴 반응 및 탈퇴 사용자의 메시지에 달린 반응 모두 삭제됨
        assertThat(messageReactionRepository.findByMessageId(leavingMessageId)).isEmpty();

        // 탈퇴 사용자를 가리키던 신고(피신고/피신고메시지) 및 탈퇴 사용자가 한 신고 모두 삭제됨
        assertThat(reportRepository.findByReportedUserId(leavingUser.getId())).isEmpty();
        assertThat(reportRepository.findByReporterId(leavingUser.getId())).isEmpty();
        assertThat(reportRepository.count()).isZero();

        // 탈퇴 사용자를 숨긴 타인 레코드(friend_id) 및 탈퇴 사용자가 숨긴 레코드(user_id) 모두 삭제됨
        assertThat(hiddenFriendRepository.findByUserId(otherUser.getId())).isEmpty();
        assertThat(hiddenFriendRepository.findByUserId(leavingUser.getId())).isEmpty();

        // 탈퇴 사용자의 Refresh Token이 (revoke가 아니라) 실제로 삭제됨.
        // 행이 남아 있으면 위 deleteAccount 호출이 fk_refresh_tokens_user 위반으로 롤백되었을 것이다.
        assertThat(refreshTokenRepository.findByToken("leaving-user-refresh-token")).isEmpty();
    }

    /**
     * 통합 테스트에서 도메인 서비스와 비밀번호 인코더 빈을 제공한다.
     */
    @TestConfiguration
    static class TestBeans {

        /**
         * 비밀번호 인코더 포트 빈.
         *
         * @return 비밀번호 인코더 포트
         */
        @Bean
        PasswordEncoderPort passwordEncoderPort() {
            return new SpringPasswordEncoderAdapter(new BCryptPasswordEncoder());
        }

        /**
         * 회원 탈퇴 서비스 빈.
         *
         * @param userRepository 사용자 레포지토리
         * @param messageRepository 메시지 레포지토리
         * @param messageReactionRepository 메시지 반응 레포지토리
         * @param chatRoomMemberRepository 채팅방 멤버 레포지토리
         * @param friendRepository 친구 레포지토리
         * @param friendRequestRepository 친구 요청 레포지토리
         * @param deviceTokenRepository 디바이스 토큰 레포지토리
         * @param passwordResetTokenRepository 비밀번호 재설정 토큰 레포지토리
         * @param emailVerificationTokenRepository 이메일 인증 토큰 레포지토리
         * @param blockRepository 차단 레포지토리
         * @param reportRepository 신고 레포지토리
         * @param hiddenFriendRepository 친구 숨김 레포지토리
         * @param notificationSettingRepository 알림 설정 레포지토리
         * @param termsAgreementRepository 약관 동의 레포지토리
         * @param refreshTokenRepository 리프레시 토큰 레포지토리
         * @param profileHistoryRepository 프로필 히스토리 레포지토리
         * @param passwordEncoder 비밀번호 인코더
         * @return 회원 탈퇴 서비스
         */
        @Bean
        DeleteAccountService deleteAccountService(
                UserRepository userRepository,
                MessageRepository messageRepository,
                MessageReactionRepository messageReactionRepository,
                com.cotalk.domain.port.outbound.ChatRoomMemberRepository chatRoomMemberRepository,
                com.cotalk.domain.port.outbound.FriendRepository friendRepository,
                com.cotalk.domain.port.outbound.FriendRequestRepository friendRequestRepository,
                com.cotalk.domain.port.outbound.DeviceTokenRepository deviceTokenRepository,
                com.cotalk.domain.port.outbound.PasswordResetTokenRepository passwordResetTokenRepository,
                com.cotalk.domain.port.outbound.EmailVerificationTokenRepository emailVerificationTokenRepository,
                com.cotalk.domain.port.outbound.BlockRepository blockRepository,
                ReportRepository reportRepository,
                HiddenFriendRepository hiddenFriendRepository,
                com.cotalk.domain.port.outbound.NotificationSettingRepository notificationSettingRepository,
                com.cotalk.domain.port.outbound.TermsAgreementRepository termsAgreementRepository,
                com.cotalk.domain.port.outbound.RefreshTokenRepository refreshTokenRepository,
                com.cotalk.domain.port.outbound.ProfileHistoryRepository profileHistoryRepository,
                PasswordEncoderPort passwordEncoder) {
            return new DeleteAccountService(
                    userRepository,
                    messageRepository,
                    messageReactionRepository,
                    chatRoomMemberRepository,
                    friendRepository,
                    friendRequestRepository,
                    deviceTokenRepository,
                    passwordResetTokenRepository,
                    emailVerificationTokenRepository,
                    blockRepository,
                    reportRepository,
                    hiddenFriendRepository,
                    notificationSettingRepository,
                    termsAgreementRepository,
                    refreshTokenRepository,
                    profileHistoryRepository,
                    passwordEncoder);
        }
    }
}
