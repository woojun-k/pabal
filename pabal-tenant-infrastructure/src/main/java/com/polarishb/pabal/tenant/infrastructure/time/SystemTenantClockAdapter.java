package com.polarishb.pabal.tenant.infrastructure.time;

import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SystemTenantClockAdapter implements ClockPort {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
