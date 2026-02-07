package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.mapper.UserMapper;
import com.cotalk.adapter.outbound.persistence.user.UserRepositoryAdapter;
import com.cotalk.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.cotalk.infrastructure.config.JpaAuditingConfig;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({UserRepositoryAdapter.class, UserMapper.class, JpaAuditingConfig.class})
@DisplayName("UserJpaRepository")
class UserJpaRepositoryTest {

    @Autowired
    private UserRepositoryAdapter userRepository;

    @Nested
    @DisplayName("저장 시")
    class Save {

        @Test
        @DisplayName("사용자를 저장하면 저장된 사용자를 반환한다")
        void should_ReturnSavedUser_when_UserSaved() {
            // given
            User user = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .passwordHash("hashedPassword")
                    .nickname("testUser")
                    .build();

            // when
            User savedUser = userRepository.save(user);

            // then
            assertThat(savedUser.getId()).isEqualTo(1L);
            assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("조회 시")
    class Find {

        @Test
        @DisplayName("ID로 사용자를 조회할 수 있다")
        void should_FindUser_when_IdProvided() {
            // given
            User user = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .passwordHash("hashedPassword")
                    .nickname("testUser")
                    .build();
            userRepository.save(user);

            // when
            Optional<User> found = userRepository.findById(1L);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("이메일로 사용자를 조회할 수 있다")
        void should_FindUser_when_EmailProvided() {
            // given
            User user = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .passwordHash("hashedPassword")
                    .nickname("testUser")
                    .build();
            userRepository.save(user);

            // when
            Optional<User> found = userRepository.findByEmail("test@example.com");

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getNickname()).isEqualTo("testUser");
        }

        @Test
        @DisplayName("닉네임으로 사용자를 검색할 수 있다")
        void should_FindUsers_when_NicknameContaining() {
            // given
            userRepository.save(User.builder()
                    .id(1L)
                    .email("test1@example.com")
                    .passwordHash("hash")
                    .nickname("testUser1")
                    .build());
            userRepository.save(User.builder()
                    .id(2L)
                    .email("test2@example.com")
                    .passwordHash("hash")
                    .nickname("testUser2")
                    .build());
            userRepository.save(User.builder()
                    .id(3L)
                    .email("other@example.com")
                    .passwordHash("hash")
                    .nickname("otherUser")
                    .build());

            // when
            List<User> found = userRepository.findByNicknameContaining("test");

            // then
            assertThat(found).hasSize(2);
        }
    }

    @Nested
    @DisplayName("존재 여부 확인 시")
    class Exists {

        @Test
        @DisplayName("이메일이 존재하면 true를 반환한다")
        void should_ReturnTrue_when_EmailExists() {
            // given
            userRepository.save(User.builder()
                    .id(1L)
                    .email("existing@example.com")
                    .passwordHash("hash")
                    .nickname("user")
                    .build());

            // when & then
            assertThat(userRepository.existsByEmail("existing@example.com")).isTrue();
            assertThat(userRepository.existsByEmail("notexist@example.com")).isFalse();
        }

        @Test
        @DisplayName("닉네임이 존재하면 true를 반환한다")
        void should_ReturnTrue_when_NicknameExists() {
            // given
            userRepository.save(User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .passwordHash("hash")
                    .nickname("existingNickname")
                    .build());

            // when & then
            assertThat(userRepository.existsByNickname("existingNickname")).isTrue();
            assertThat(userRepository.existsByNickname("notExist")).isFalse();
        }
    }
}
