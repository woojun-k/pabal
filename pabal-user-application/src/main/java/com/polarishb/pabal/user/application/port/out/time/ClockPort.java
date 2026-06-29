package com.polarishb.pabal.user.application.port.out.time;

import java.time.Instant;

public interface ClockPort {

    Instant now();
}
