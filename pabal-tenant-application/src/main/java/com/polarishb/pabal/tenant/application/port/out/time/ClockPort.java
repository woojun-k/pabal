package com.polarishb.pabal.tenant.application.port.out.time;

import java.time.Instant;

public interface ClockPort {
    Instant now();
}
