package com.cotalk.infrastructure.id;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SnowflakeIdGenerator")
class SnowflakeIdGeneratorTest {

    private SnowflakeIdGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SnowflakeIdGenerator(1, 1);
    }

    @Nested
    @DisplayName("ID 생성 시")
    class GenerateId {

        @Test
        @DisplayName("양수 Long 값을 반환한다")
        void should_ReturnPositiveLong_when_Generated() {
            // when
            long id = generator.nextId();

            // then
            assertThat(id).isPositive();
        }

        @Test
        @DisplayName("순차적으로 생성된 ID는 증가한다")
        void should_BeIncreasing_when_GeneratedSequentially() {
            // when
            long id1 = generator.nextId();
            long id2 = generator.nextId();
            long id3 = generator.nextId();

            // then
            assertThat(id2).isGreaterThan(id1);
            assertThat(id3).isGreaterThan(id2);
        }

        @Test
        @DisplayName("동일한 시점에 생성해도 고유한 ID를 반환한다")
        void should_BeUnique_when_GeneratedAtSameTime() {
            // given
            Set<Long> ids = new HashSet<>();
            int count = 1000;

            // when
            for (int i = 0; i < count; i++) {
                ids.add(generator.nextId());
            }

            // then
            assertThat(ids).hasSize(count);
        }
    }

    @Nested
    @DisplayName("멀티스레드 환경에서")
    class MultiThreaded {

        @Test
        @DisplayName("동시에 생성해도 고유한 ID를 반환한다")
        void should_BeUnique_when_GeneratedConcurrently() throws InterruptedException {
            // given
            int threadCount = 10;
            int idsPerThread = 1000;
            Set<Long> ids = java.util.Collections.synchronizedSet(new HashSet<>());
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            // when
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < idsPerThread; j++) {
                            ids.add(generator.nextId());
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            executor.shutdown();

            // then
            assertThat(ids).hasSize(threadCount * idsPerThread);
        }
    }

    @Nested
    @DisplayName("ID 구조 검증")
    class IdStructure {

        @Test
        @DisplayName("타임스탬프를 추출할 수 있다")
        void should_ExtractTimestamp_when_IdGenerated() {
            // given
            long beforeGeneration = System.currentTimeMillis();
            long id = generator.nextId();
            long afterGeneration = System.currentTimeMillis();

            // when
            long timestamp = generator.getTimestamp(id);

            // then
            assertThat(timestamp).isBetween(beforeGeneration, afterGeneration);
        }
    }
}
