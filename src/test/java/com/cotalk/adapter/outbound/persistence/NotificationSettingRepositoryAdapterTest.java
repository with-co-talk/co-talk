package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.notification.NotificationSettingRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.mapper.UserMapper;
import com.cotalk.adapter.outbound.persistence.user.UserRepositoryAdapter;
import com.cotalk.domain.entity.NotificationSetting;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.infrastructure.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NotificationSettingRepositoryAdapter 테스트.
 *
 * @author seunggu.lee
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({NotificationSettingRepositoryAdapter.class, UserRepositoryAdapter.class, UserMapper.class, JpaAuditingConfig.class})
@DisplayName("NotificationSettingRepositoryAdapter")
class NotificationSettingRepositoryAdapterTest {

    @Autowired
    private NotificationSettingRepositoryAdapter notificationSettingRepository;

    @Autowired
    private UserRepositoryAdapter userRepository;

    private User user1;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder()
                .id(1L)
                .email(new Email("user1@example.com"))
                .passwordHash("hash")
                .nickname("user1")
                .build());
    }

    @Nested
    @DisplayName("저장 시")
    class Save {

        @Test
        @DisplayName("알림 설정을 저장한다")
        void should_saveSetting_when_settingProvided() {
            // given
            NotificationSetting setting = NotificationSetting.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .messageNotification(true)
                    .friendRequestNotification(true)
                    .groupInviteNotification(false)
                    .soundEnabled(true)
                    .vibrationEnabled(false)
                    .build();

            // when
            NotificationSetting saved = notificationSettingRepository.save(setting);

            // then
            assertThat(saved.getUserId()).isEqualTo(user1.getId());
            assertThat(saved.isMessageNotification()).isTrue();
            assertThat(saved.isFriendRequestNotification()).isTrue();
            assertThat(saved.isGroupInviteNotification()).isFalse();
        }

        @Test
        @DisplayName("방해금지 시간을 설정하여 저장한다")
        void should_saveSettingWithDoNotDisturb_when_timeProvided() {
            // given
            NotificationSetting setting = NotificationSetting.builder()
                    .id(101L)
                    .userId(user1.getId())
                    .doNotDisturbEnabled(true)
                    .doNotDisturbStart("22:00")
                    .doNotDisturbEnd("07:00")
                    .build();

            // when
            NotificationSetting saved = notificationSettingRepository.save(setting);

            // then
            assertThat(saved.isDoNotDisturbEnabled()).isTrue();
            assertThat(saved.getDoNotDisturbStart()).isEqualTo("22:00");
            assertThat(saved.getDoNotDisturbEnd()).isEqualTo("07:00");
        }
    }

    @Nested
    @DisplayName("조회 시")
    class Find {

        @Test
        @DisplayName("사용자 ID로 알림 설정을 조회한다")
        void should_findSetting_when_userIdProvided() {
            // given
            notificationSettingRepository.save(NotificationSetting.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .messageNotification(true)
                    .build());

            // when
            Optional<NotificationSetting> found = notificationSettingRepository
                    .findByUserId(user1.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().isMessageNotification()).isTrue();
        }

        @Test
        @DisplayName("알림 설정이 없으면 빈 Optional을 반환한다")
        void should_returnEmpty_when_settingNotFound() {
            // when
            Optional<NotificationSetting> found = notificationSettingRepository
                    .findByUserId(999L);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("업데이트 시")
    class Update {

        @Test
        @DisplayName("알림 설정을 업데이트한다")
        void should_updateSetting_when_settingModified() {
            // given
            NotificationSetting setting = notificationSettingRepository.save(
                    NotificationSetting.builder()
                            .id(100L)
                            .userId(user1.getId())
                            .messageNotification(true)
                            .soundEnabled(true)
                            .build());

            // when
            setting.updateMessageNotification(false);
            setting.updateSoundEnabled(false);
            NotificationSetting updated = notificationSettingRepository.save(setting);

            // then
            assertThat(updated.isMessageNotification()).isFalse();
            assertThat(updated.isSoundEnabled()).isFalse();
        }
    }
}
