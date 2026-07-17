package com.polarishb.pabal.integration.contract.dto;

import java.util.UUID;

public record UserInfo(
    UUID userId,
    String name,
    UUID tenantId
) {}
