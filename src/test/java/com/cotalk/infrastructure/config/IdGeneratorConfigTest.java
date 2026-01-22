package com.cotalk.infrastructure.config;

import com.cotalk.config.TestRedisConfiguration;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class IdGeneratorConfigTest {

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Test
    @DisplayName("SnowflakeIdGenerator 빈이 정상적으로 생성됨")
    void should_createSnowflakeIdGeneratorBean() {
        // then
        assertThat(snowflakeIdGenerator).isNotNull();
    }

    @Test
    @DisplayName("SnowflakeIdGenerator가 유효한 ID를 생성함")
    void should_generateValidId_when_nextIdCalled() {
        // when
        Long id = snowflakeIdGenerator.nextId();

        // then
        assertThat(id).isPositive();
    }

    @Test
    @DisplayName("SnowflakeIdGenerator가 고유한 ID를 생성함")
    void should_generateUniqueIds_when_multipleNextIdCalled() {
        // when
        Long id1 = snowflakeIdGenerator.nextId();
        Long id2 = snowflakeIdGenerator.nextId();
        Long id3 = snowflakeIdGenerator.nextId();

        // then
        assertThat(id1).isNotEqualTo(id2);
        assertThat(id2).isNotEqualTo(id3);
        assertThat(id1).isNotEqualTo(id3);
    }
}
