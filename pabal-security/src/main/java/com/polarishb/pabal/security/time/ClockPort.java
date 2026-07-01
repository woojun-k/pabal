package com.polarishb.pabal.security.time;

import java.time.Instant;

public interface ClockPort {

    Instant now();
}
