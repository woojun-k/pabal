package com.polarishb.pabal.common.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.locks.LockSupport.parkNanos;

public final class UuidV7 {
    private static final long TS_MASK_48 = 0xFFFFFFFFFFFFL;          // 48 bits
    private static final int RAND_A_MASK_12 = 0x0FFF;                  // 12 bits
    private static final long RAND_B_MASK_62 = 0x3FFFFFFFFFFFFFFFL;  // 62 bits

    private static final long VERSION_7 = 0x7L;
    private static final long VARIANT_RFC4122 = 0x8000000000000000L;

    private static final int MAX_SPIN_ATTEMPTS = Integer.getInteger("uuidv7.max.spin", 1000);

    private static final SecureRandom RNG = new SecureRandom();
    static {
        RNG.nextBytes(new byte[1]);
    }

    private static final AtomicLong LAST_STATE = new AtomicLong(0);

    private UuidV7() {}

    public static UUID random() {
        long ts = System.currentTimeMillis() & TS_MASK_48;
        int randA = RNG.nextInt(1 << 12);
        long randB = nextRandB62();
        return build(ts, randA, randB);
    }

    public static UUID monotonic() {
        long ts = System.currentTimeMillis() & TS_MASK_48;
        int randA;
        long randB;

        while (true) {
            long prev = LAST_STATE.get();
            long prevTs = (prev >>> 12) & TS_MASK_48;
            int prevRandA = (int) (prev & RAND_A_MASK_12);

            if (ts > prevTs) {
                // 시간이 흘렀으면 새로운 랜덤 시드
                randA = RNG.nextInt(1 << 12);
            } else {
                // 동일 밀리초거나 클락 롤백 시 시퀀스 증가
                ts = prevTs;
                randA = (prevRandA + 1) & RAND_A_MASK_12;

                // 시퀀스 오버플로 시 다음 ms까지 대기
                if (randA == 0) {
                    int spinCount = 0;
                    do {
                        if (++spinCount > MAX_SPIN_ATTEMPTS) {
                            throw new IllegalStateException(
                                    "UUID generation rate exceeded (timestamp frozen at " + prevTs + ")"
                            );
                        }
                        if ((spinCount & 0xFF) == 0) {
                            parkNanos(100_000); // 0.1ms
                        } else {
                            Thread.onSpinWait();
                        }
                        ts = System.currentTimeMillis() & TS_MASK_48;
                    } while (ts <= prevTs);
                    randA = RNG.nextInt(1 << 12);
                }
            }

            long next = (ts << 12) | (randA & RAND_A_MASK_12);
            if (LAST_STATE.compareAndSet(prev, next)) {
                break;
            }

            Thread.onSpinWait();
            ts = System.currentTimeMillis() & TS_MASK_48;
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
        return RNG.nextLong() & RAND_B_MASK_62;
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
}
