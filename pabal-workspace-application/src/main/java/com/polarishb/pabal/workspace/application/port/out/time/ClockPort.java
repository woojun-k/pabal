package com.polarishb.pabal.workspace.application.port.out.time;

import java.time.Instant;

public interface ClockPort {
    Instant now();
}
