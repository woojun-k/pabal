package com.polarishb.pabal.messenger.application.port.out.time;

import java.time.Instant;

public interface ClockPort {

    Instant now();
}
