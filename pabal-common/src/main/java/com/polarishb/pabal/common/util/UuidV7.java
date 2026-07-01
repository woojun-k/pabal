package com.polarishb.pabal.common.util;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static java.util.concurrent.locks.LockSupport.parkNanos;

public final class UuidV7 {
    private static final long TS_MASK_48 = 0xFFFFFFFFFFFFL;          // 48 bits
    private static final int RAND_A_MASK_12 = 0x0FFF;                  // 12 bits
    private static final long RAND_B_MASK_62 = 0x3FFFFFFFFFFFFFFFL;  // 62 bits

    private static final long VERSION_7 = 0x7L;
    private static final long VARIANT_RFC4122 = 0x8000000000000000L;

    // 카운터 기반이 아닌 경과시간 기반: ms 클럭 해상도(최악 ~1ms)를 확실히 넘는 여유(2ms)를
    // 확보해야 시퀀스 오버플로 대기 중 클럭 틱을 놓쳐 스퓨리어스 예외가 나는 것을 막을 수 있다.
    private static final long SPIN_DEADLINE_NANOS = Long.getLong("uuidv7.spin.deadline.nanos", 2_000_000L);

    private static final AtomicLong LAST_STATE = new AtomicLong(0);
    private static volatile LongSupplier clockMillis = System::currentTimeMillis;

    private UuidV7() {}

    // ID 생성 hot path에서는 예측 불가능성(비암호학적)만 있으면 충분하다. 공유 암호학적
    // 난수 생성기는 내부 synchronized 진입점 때문에 스레드 경합을 유발하므로, 스레드별
    // 인스턴스인 ThreadLocalRandom.current() 로 대체해 락 프리(lock-free) 경로를 확보한다.
    // 보안 토큰 등 암호학적 난수가 필요한 곳은 pabal-security 모듈 쪽 구현을 그대로 사용한다.

    public static UUID random() {
        long ts = clockMillis.getAsLong() & TS_MASK_48;
        int randA = ThreadLocalRandom.current().nextInt(1 << 12);
        long randB = nextRandB62();
        return build(ts, randA, randB);
    }

    public static UUID monotonic() {
        long ts = clockMillis.getAsLong() & TS_MASK_48;
        int randA;
        long randB;

        while (true) {
            long prev = LAST_STATE.get();
            long prevTs = (prev >>> 12) & TS_MASK_48;
            int prevRandA = (int) (prev & RAND_A_MASK_12);

            if (ts > prevTs) {
                // 시간이 흘렀으면 새로운 랜덤 시드
                randA = ThreadLocalRandom.current().nextInt(1 << 12);
            } else {
                // 동일 밀리초거나 클락 롤백 시 시퀀스 증가
                ts = prevTs;
                randA = (prevRandA + 1) & RAND_A_MASK_12;

                // 시퀀스 오버플로 시 다음 ms까지 대기
                if (randA == 0) {
                    long deadline = System.nanoTime() + SPIN_DEADLINE_NANOS;
                    int spinCount = 0;
                    do {
                        if (System.nanoTime() >= deadline) {
                            throw new IllegalStateException(
                                    "UUID generation rate exceeded (timestamp frozen at " + prevTs + ")"
                            );
                        }
                        spinCount++;
                        if ((spinCount & 0xFF) == 0) {
                            parkNanos(100_000); // 0.1ms
                        } else {
                            Thread.onSpinWait();
                        }
                        ts = clockMillis.getAsLong() & TS_MASK_48;
                    } while (ts <= prevTs);
                    randA = ThreadLocalRandom.current().nextInt(1 << 12);
                }
            }

            long next = (ts << 12) | (randA & RAND_A_MASK_12);
            if (LAST_STATE.compareAndSet(prev, next)) {
                break;
            }

            Thread.onSpinWait();
            ts = clockMillis.getAsLong() & TS_MASK_48;
        }

        randB = nextRandB62();

        return build(ts, randA, randB);
    }

    public static long timestampMillis(UUID uuidV7) {
        Objects.requireNonNull(uuidV7, "uuidV7");
        ensureV7(uuidV7);
        long msb = uuidV7.getMostSignificantBits();
        return (msb >>> 16) & TS_MASK_48;
    }

    public static Instant timestampInstant(UUID uuidV7) {
        return Instant.ofEpochMilli(timestampMillis(uuidV7));
    }

    public static boolean isV7(UUID u) {
        if (u == null) return false;
        if (u.variant() != 2) return false; // RFC 4122 variant
        return u.version() == 7;
    }

    private static void ensureV7(UUID u) {
        if (u.variant() != 2 || u.version() != 7) {
            throw new IllegalArgumentException(
                    "Not a RFC4122-variant UUIDv7: " + u
            );
        }
    }

    private static long nextRandB62() {
        return ThreadLocalRandom.current().nextLong() & RAND_B_MASK_62;
    }

    private static UUID build(long unixTsMs, int randA12, long randB62) {
        long ts48 = unixTsMs & TS_MASK_48;

        // msb: [timestamp(48) | version(4) | rand_a(12)]
        long msb = (ts48 << 16)
                | (VERSION_7 << 12)
                | (randA12 & RAND_A_MASK_12);

        // lsb: [variant(2) | rand_b(62)]
        long lsb = VARIANT_RFC4122 | (randB62 & RAND_B_MASK_62);

        return new UUID(msb, lsb);
    }

    public static int compare(UUID left, UUID right) {
        int msbCmp = Long.compareUnsigned(left.getMostSignificantBits(), right.getMostSignificantBits());
        if (msbCmp != 0) return msbCmp;
        return Long.compareUnsigned(left.getLeastSignificantBits(), right.getLeastSignificantBits());
    }

    // ---------------------------------------------------------------------
    // 테스트 전용
    // ---------------------------------------------------------------------

    /** 밀리초 시간 소스를 교체한다. 테스트 종료 시 반드시 {@link #resetForTest()} 로 복구할 것. */
    static void overrideClockForTest(LongSupplier source) {
        clockMillis = source;
    }

    /** {@code LAST_STATE} 를 특정 (timestamp, randA) 상태로 시드해 후속 호출 분기를 강제한다. */
    static void seedStateForTest(long tsMillis, int randA12) {
        LAST_STATE.set(((tsMillis & TS_MASK_48) << 12) | (randA12 & RAND_A_MASK_12));
    }

    /** 시간 소스와 단조 상태를 프로덕션 기본값으로 되돌린다. */
    static void resetForTest() {
        clockMillis = System::currentTimeMillis;
        LAST_STATE.set(0L);
    }
}
