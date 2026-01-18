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

    @Nested
    @DisplayName("생성자 검증")
    class Constructor {

        @Test
        @DisplayName("유효한 데이터센터 ID와 워커 ID로 생성 성공")
        void should_CreateGenerator_when_ValidIds() {
            // given & when
            SnowflakeIdGenerator gen = new SnowflakeIdGenerator(0, 0);

            // then
            assertThat(gen.nextId()).isPositive();
        }

        @Test
        @DisplayName("최대 데이터센터 ID(31)로 생성 성공")
        void should_CreateGenerator_when_MaxDatacenterId() {
            // given & when
            SnowflakeIdGenerator gen = new SnowflakeIdGenerator(31, 0);

            // then
            assertThat(gen.nextId()).isPositive();
        }

        @Test
        @DisplayName("최대 워커 ID(31)로 생성 성공")
        void should_CreateGenerator_when_MaxWorkerId() {
            // given & when
            SnowflakeIdGenerator gen = new SnowflakeIdGenerator(0, 31);

            // then
            assertThat(gen.nextId()).isPositive();
        }

        @Test
        @DisplayName("데이터센터 ID가 음수이면 예외 발생")
        void should_ThrowException_when_NegativeDatacenterId() {
            // when & then
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new SnowflakeIdGenerator(-1, 0)
            );
        }

        @Test
        @DisplayName("데이터센터 ID가 31 초과이면 예외 발생")
        void should_ThrowException_when_DatacenterIdExceedsMax() {
            // when & then
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new SnowflakeIdGenerator(32, 0)
            );
        }

        @Test
        @DisplayName("워커 ID가 음수이면 예외 발생")
        void should_ThrowException_when_NegativeWorkerId() {
            // when & then
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new SnowflakeIdGenerator(0, -1)
            );
        }

        @Test
        @DisplayName("워커 ID가 31 초과이면 예외 발생")
        void should_ThrowException_when_WorkerIdExceedsMax() {
            // when & then
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new SnowflakeIdGenerator(0, 32)
            );
        }
    }

    @Nested
    @DisplayName("시계 역행 처리")
    class ClockBackwards {

        @Test
        @DisplayName("시계가 역행하면 예외 발생")
        void should_ThrowException_when_ClockMovesBackwards() {
            // given
            TestableSnowflakeIdGenerator testGenerator = new TestableSnowflakeIdGenerator(1, 1);
            testGenerator.setCurrentTime(1000L);
            testGenerator.nextId(); // 첫 번째 ID 생성

            // when
            testGenerator.setCurrentTime(500L); // 시계 역행

            // then
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalStateException.class,
                    testGenerator::nextId
            );
        }
    }

    @Nested
    @DisplayName("시퀀스 오버플로우")
    class SequenceOverflow {

        @Test
        @DisplayName("같은 밀리초 내 시퀀스 증가")
        void should_IncrementSequence_when_SameMillisecond() {
            // given
            TestableSnowflakeIdGenerator testGenerator = new TestableSnowflakeIdGenerator(1, 1);
            testGenerator.setCurrentTime(1000L);

            // when
            long id1 = testGenerator.nextId();
            long id2 = testGenerator.nextId();

            // then
            assertThat(id2).isGreaterThan(id1);
        }

        @Test
        @DisplayName("새 밀리초에서 시퀀스 리셋")
        void should_ResetSequence_when_NewMillisecond() {
            // given
            TestableSnowflakeIdGenerator testGenerator = new TestableSnowflakeIdGenerator(1, 1);
            testGenerator.setCurrentTime(1000L);
            testGenerator.nextId();

            // when
            testGenerator.setCurrentTime(1001L);
            long id = testGenerator.nextId();

            // then - 시퀀스가 0으로 리셋되었는지 확인 (ID의 마지막 12비트)
            long sequence = id & 0xFFF;
            assertThat(sequence).isZero();
        }
    }

    @Nested
    @DisplayName("다른 데이터센터/워커 조합")
    class DifferentDatacenterWorker {

        @Test
        @DisplayName("다른 데이터센터 ID는 다른 ID 생성")
        void should_GenerateDifferentId_when_DifferentDatacenterId() {
            // given
            SnowflakeIdGenerator gen1 = new SnowflakeIdGenerator(1, 1);
            SnowflakeIdGenerator gen2 = new SnowflakeIdGenerator(2, 1);

            // when
            long id1 = gen1.nextId();
            long id2 = gen2.nextId();

            // then
            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("다른 워커 ID는 다른 ID 생성")
        void should_GenerateDifferentId_when_DifferentWorkerId() {
            // given
            SnowflakeIdGenerator gen1 = new SnowflakeIdGenerator(1, 1);
            SnowflakeIdGenerator gen2 = new SnowflakeIdGenerator(1, 2);

            // when
            long id1 = gen1.nextId();
            long id2 = gen2.nextId();

            // then
            assertThat(id1).isNotEqualTo(id2);
        }
    }

    /**
     * 테스트용 SnowflakeIdGenerator.
     * currentTimeMillis를 오버라이드하여 시간을 조작할 수 있다.
     */
    private static class TestableSnowflakeIdGenerator extends SnowflakeIdGenerator {
        private long currentTime = System.currentTimeMillis();

        public TestableSnowflakeIdGenerator(long datacenterId, long workerId) {
            super(datacenterId, workerId);
        }

        public void setCurrentTime(long time) {
            this.currentTime = time;
        }

        @Override
        protected long currentTimeMillis() {
            return currentTime;
        }
    }
}
