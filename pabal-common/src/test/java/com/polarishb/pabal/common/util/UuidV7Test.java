package com.polarishb.pabal.common.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UuidV7Test {

    private static final long RAND_A_MASK_12 = 0x0FFFL; // msb 하위 12비트(rand_a / 시퀀스)

    @AfterEach
    void restoreProductionState() {
        // 정적 시간 소스/단조 상태가 다음 테스트로 새지 않도록 항상 프로덕션 기본값으로 복구.
        UuidV7.resetForTest();
    }

    @Nested
    @DisplayName("UuidV7.compare()")
    class CompareTest {

        private static final UUID UUID_MSB_HIGH_BIT_SET =
                new UUID(0x8000000000000000L, 0x0000000000000001L); // MSB 최상위 비트 = 1

        private static final UUID UUID_MSB_HIGH_BIT_CLEAR =
                new UUID(0x7FFFFFFFFFFFFFFFL, 0x0000000000000001L); // MSB 최상위 비트 = 0

        @Test
        void msbHighBitSet_isGreaterThan_msbHighBitClear_inUnsignedOrder() {
            assertThat(UuidV7.compare(UUID_MSB_HIGH_BIT_SET, UUID_MSB_HIGH_BIT_CLEAR)).isPositive();
        }

        @Test
        void signedVsUnsigned_orderDiffers_whenMsbHighBitIsSet() {
            int signedResult = UUID_MSB_HIGH_BIT_SET.compareTo(UUID_MSB_HIGH_BIT_CLEAR);
            int unsignedResult = UuidV7.compare(UUID_MSB_HIGH_BIT_SET, UUID_MSB_HIGH_BIT_CLEAR);

            assertThat(signedResult).isNegative();
            assertThat(unsignedResult).isPositive();
            assertThat(Integer.signum(signedResult)).isNotEqualTo(Integer.signum(unsignedResult));
        }

        @Test
        void sameUuid_returnsZero() {
            assertThat(UuidV7.compare(UUID_MSB_HIGH_BIT_SET, UUID_MSB_HIGH_BIT_SET)).isZero();
        }
    }

    @Nested
    @DisplayName("UuidV7.monotonic() - 구조 및 단일 스레드 순서")
    class MonotonicStructureAndOrderingTest {

        @Test
        @DisplayName("생성된 값은 RFC4122 variant 의 v7 이다")
        void output_isRfc4122VariantV7() {
            UUID u = UuidV7.monotonic();

            assertThat(u.version()).isEqualTo(7);
            assertThat(u.variant()).isEqualTo(2);
            assertThat(UuidV7.isV7(u)).isTrue();
        }

        @Test
        @DisplayName("고정 클럭에서 타임스탬프가 왕복 추출된다")
        void timestamp_roundTrips_underFixedClock() {
            long fixed = 1_700_000_000_000L;
            UuidV7.overrideClockForTest(() -> fixed);

            UUID u = UuidV7.monotonic();

            assertThat(UuidV7.timestampMillis(u)).isEqualTo(fixed);
        }

        @Test
        @DisplayName("단일 스레드 연속 생성은 엄격히 증가한다(동일 ms 시퀀스 증가 경로 포함)")
        void sequentialGeneration_isStrictlyIncreasing() {
            UUID prev = UuidV7.monotonic();

            for (int i = 0; i < 20_000; i++) {
                UUID next = UuidV7.monotonic();
                assertThat(UuidV7.compare(next, prev))
                        .as("index %d 에서 이전 값보다 커야 한다", i)
                        .isPositive();
                prev = next;
            }
        }
    }

    @Nested
    @DisplayName("UuidV7.monotonic() - 동시성(CAS 루프)")
    class ConcurrencyTest {

        @Test
        @DisplayName("멀티 스레드 동시 생성 시 중복이 발생하지 않는다")
        void concurrentGeneration_producesNoDuplicates() throws Exception {
            int threads = 12;
            int perThread = 5_000;
            int total = threads * perThread;

            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch startGate = new CountDownLatch(1);
            ConcurrentHashMap.KeySetView<UUID, Boolean> produced = ConcurrentHashMap.newKeySet(total);
            AtomicInteger duplicates = new AtomicInteger();
            AtomicInteger rateLimited = new AtomicInteger(); // 합성 폭주로 인한 초과(정상 하드웨어에서는 0)

            List<Future<?>> futures = new ArrayList<>(threads);
            try {
                for (int t = 0; t < threads; t++) {
                    futures.add(pool.submit(() -> {
                        startGate.await();
                        for (int i = 0; i < perThread; i++) {
                            try {
                                if (!produced.add(UuidV7.monotonic())) {
                                    duplicates.incrementAndGet();
                                }
                            } catch (IllegalStateException rateExceeded) {
                                rateLimited.incrementAndGet();
                            }
                        }
                        return null;
                    }));
                }
                startGate.countDown();
                for (Future<?> f : futures) {
                    f.get(60, TimeUnit.SECONDS);
                }
            } finally {
                pool.shutdownNow();
            }

            // 핵심 불변식: CAS 루프가 어떤 두 스레드에도 동일 값을 내주지 않는다.
            assertThat(duplicates).hasValue(0);
            // 생성 성공분은 전부 고유해야 하며, 성공 개수 = 전체 - (합성 폭주로 인한 초과).
            assertThat(produced).hasSize(total - rateLimited.get());
        }
    }

    @Nested
    @DisplayName("UuidV7.monotonic() - 클럭 롤백 / 시퀀스 오버플로")
    class ClockRollbackAndOverflowTest {

        @Test
        @DisplayName("클럭이 롤백돼도 타임스탬프는 뒤로 가지 않고 시퀀스만 증가한다")
        void clockRollback_neverGoesBackwards() {
            long lastTs = 1_000_000L;
            UuidV7.seedStateForTest(lastTs, 5);      // 직전 상태: ts=lastTs, seq=5
            UuidV7.overrideClockForTest(() -> lastTs - 10); // 클럭이 10ms 뒤로 롤백

            UUID next = UuidV7.monotonic();

            // 타임스탬프는 롤백된 값(lastTs-10)이 아니라 직전 값으로 고정된다.
            assertThat(UuidV7.timestampMillis(next)).isEqualTo(lastTs);
            // 시퀀스(rand_a 하위 12비트)는 5 -> 6 으로 증가.
            assertThat(next.getMostSignificantBits() & RAND_A_MASK_12).isEqualTo(6L);
        }

        @Test
        @DisplayName("시퀀스 오버플로 시 다음 ms 를 기다렸다가 전진한다")
        void sequenceOverflow_waitsForNextMillisecond() {
            long frozenTs = 2_000_000L;
            UuidV7.seedStateForTest(frozenTs, 0x0FFF); // 시퀀스가 최대치

            // 첫 읽기는 같은 ms(오버플로 유발), 이후 읽기는 다음 ms 도착.
            AtomicInteger reads = new AtomicInteger();
            UuidV7.overrideClockForTest(() -> reads.getAndIncrement() == 0 ? frozenTs : frozenTs + 1);

            UUID next = UuidV7.monotonic();

            assertThat(UuidV7.timestampMillis(next)).isEqualTo(frozenTs + 1);
            assertThat(next.version()).isEqualTo(7);
        }

        @Test
        @DisplayName("오버플로 상태에서 클럭이 전혀 전진하지 않으면 스핀 한도 초과로 예외를 던진다")
        void sequenceOverflow_whenClockNeverAdvances_throws() {
            long frozenTs = 3_000_000L;
            UuidV7.seedStateForTest(frozenTs, 0x0FFF);
            UuidV7.overrideClockForTest(() -> frozenTs); // 절대 전진하지 않는 클럭

            assertThatThrownBy(UuidV7::monotonic)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("rate exceeded");
        }
    }

    @Nested
    @DisplayName("UuidV7.random() - 구조")
    class RandomStructureTest {

        @Test
        @DisplayName("생성된 값은 RFC4122 variant 의 v7 이다")
        void output_isRfc4122VariantV7() {
            UUID u = UuidV7.random();

            assertThat(u.version()).isEqualTo(7);
            assertThat(u.variant()).isEqualTo(2);
            assertThat(UuidV7.isV7(u)).isTrue();
        }

        @Test
        @DisplayName("고정 클럭에서 타임스탬프가 왕복 추출된다")
        void timestamp_roundTrips_underFixedClock() {
            long fixed = 1_700_000_000_000L;
            UuidV7.overrideClockForTest(() -> fixed);

            UUID u = UuidV7.random();

            assertThat(UuidV7.timestampMillis(u)).isEqualTo(fixed);
        }

        @Test
        @DisplayName("random() 호출은 monotonic() 의 단조 순서 상태를 변경하지 않는다")
        void random_doesNotMutateMonotonicOrderingState() {
            long ts = 5_000_000L;
            UuidV7.seedStateForTest(ts, 10);
            UuidV7.overrideClockForTest(() -> ts);

            for (int i = 0; i < 100; i++) {
                UuidV7.random();
            }

            UUID next = UuidV7.monotonic();

            // random() 이 LAST_STATE 를 건드리지 않았다면, monotonic() 은 seed 된 randA(10)에서
            // 정확히 1 만큼 증가한 값을 내야 한다(같은 ms 이므로 시퀀스 증가 경로).
            assertThat(UuidV7.timestampMillis(next)).isEqualTo(ts);
            assertThat(next.getMostSignificantBits() & RAND_A_MASK_12).isEqualTo(11L);
        }
    }

    @Nested
    @DisplayName("UuidV7.random() - 동시성 스모크")
    class RandomConcurrencyTest {

        @Test
        @DisplayName("멀티 스레드에서 동시 호출해도 예외 없이 유효한 v7 UUID 를 생성한다")
        void concurrentGeneration_producesValidV7UuidsWithoutContentionFailures() throws Exception {
            int threads = 12;
            int perThread = 5_000;

            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch startGate = new CountDownLatch(1);
            AtomicInteger invalid = new AtomicInteger();

            List<Future<?>> futures = new ArrayList<>(threads);
            try {
                for (int t = 0; t < threads; t++) {
                    futures.add(pool.submit(() -> {
                        startGate.await();
                        for (int i = 0; i < perThread; i++) {
                            UUID u = UuidV7.random();
                            if (!UuidV7.isV7(u)) {
                                invalid.incrementAndGet();
                            }
                        }
                        return null;
                    }));
                }
                startGate.countDown();
                for (Future<?> f : futures) {
                    f.get(60, TimeUnit.SECONDS);
                }
            } finally {
                pool.shutdownNow();
            }

            assertThat(invalid).hasValue(0);
        }
    }

    @Nested
    @DisplayName("타임스탬프/검증 - 실패 모드 보존")
    class ValidationFailureModeTest {

        @Test
        @DisplayName("null UUID 로 timestampMillis 호출 시 NullPointerException")
        void timestampMillis_throwsNpe_forNullUuid() {
            assertThatThrownBy(() -> UuidV7.timestampMillis(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("v7 이 아닌 UUID 로 timestampMillis 호출 시 IllegalArgumentException")
        void timestampMillis_throwsIllegalArgumentException_forNonV7Uuid() {
            UUID v4 = UUID.randomUUID(); // JDK 기본 randomUUID() 는 version 4

            assertThatThrownBy(() -> UuidV7.timestampMillis(v4))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("v7 이 아닌 UUID 로 timestampInstant 호출 시 IllegalArgumentException")
        void timestampInstant_throwsIllegalArgumentException_forNonV7Uuid() {
            UUID v4 = UUID.randomUUID();

            assertThatThrownBy(() -> UuidV7.timestampInstant(v4))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("isV7 은 null 에 대해 false 를 반환한다")
        void isV7_returnsFalse_forNull() {
            assertThat(UuidV7.isV7(null)).isFalse();
        }

        @Test
        @DisplayName("isV7 은 v7 이 아닌 UUID 에 대해 false 를 반환한다")
        void isV7_returnsFalse_forNonV7Uuid() {
            UUID v4 = UUID.randomUUID();

            assertThat(UuidV7.isV7(v4)).isFalse();
        }
    }

    @Nested
    @DisplayName("UuidV7 - 공유 SecureRandom 제거(정적 검증)")
    class SharedRandomRemovalTest {

        private static final Path UUID_V7_SOURCE = Path.of(System.getProperty("user.dir"))
                .resolve("src/main/java/com/polarishb/pabal/common/util/UuidV7.java");

        @Test
        @DisplayName("UuidV7 소스는 SecureRandom 을 import/선언/호출하지 않는다")
        void sourceHasNoSecureRandomReference() throws Exception {
            assertThat(Files.exists(UUID_V7_SOURCE))
                    .as("UuidV7.java 소스 파일을 찾을 수 없다: %s", UUID_V7_SOURCE)
                    .isTrue();

            String source = Files.readString(UUID_V7_SOURCE);

            assertThat(source).doesNotContain("SecureRandom");
        }

        @Test
        @DisplayName("UuidV7 은 모든 스레드가 공유하는 정적 Random(하위 타입 포함) 필드를 갖지 않는다")
        void noStaticSharedRandomField() {
            List<Field> sharedRandomFields = new ArrayList<>();
            for (Field field : UuidV7.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && Random.class.isAssignableFrom(field.getType())) {
                    sharedRandomFields.add(field);
                }
            }

            assertThat(sharedRandomFields)
                    .as("ID 생성 hot path 에서 모든 스레드가 공유하는 정적 RNG 필드가 없어야 한다")
                    .isEmpty();
        }
    }
}
