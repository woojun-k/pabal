package com.polarishb.pabal.messenger.infrastructure.time;

import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SystemClockAdapter implements ClockPort {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
