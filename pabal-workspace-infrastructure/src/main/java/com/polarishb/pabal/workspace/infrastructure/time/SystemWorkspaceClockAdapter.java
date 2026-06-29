package com.polarishb.pabal.workspace.infrastructure.time;

import com.polarishb.pabal.workspace.application.port.out.time.ClockPort;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SystemWorkspaceClockAdapter implements ClockPort {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
