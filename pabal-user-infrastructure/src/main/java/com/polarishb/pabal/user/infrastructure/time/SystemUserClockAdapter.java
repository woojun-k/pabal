package com.polarishb.pabal.user.infrastructure.time;

import com.polarishb.pabal.user.application.port.out.time.ClockPort;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SystemUserClockAdapter implements ClockPort {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
