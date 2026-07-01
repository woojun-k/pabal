package com.polarishb.pabal.security.time;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SystemSecurityClockAdapter implements ClockPort {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
