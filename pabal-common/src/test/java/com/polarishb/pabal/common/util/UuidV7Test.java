package com.polarishb.pabal.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class UuidV7Test {

    private static final UUID UUID_MSB_HIGH_BIT_SET =
            new UUID(0x8000000000000000L, 0x0000000000000001L); // MSB 최상위 비트 = 1

    private static final UUID UUID_MSB_HIGH_BIT_CLEAR =
            new UUID(0x7FFFFFFFFFFFFFFFL, 0x0000000000000001L); // MSB 최상위 비트 = 0

    @Nested
    @DisplayName("UuidV7.compare()")
    class CompareTest {

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
}
