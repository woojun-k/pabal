package com.polarishb.pabal.common.contract.dto;

import java.util.UUID;

public record UserInfo(
    UUID userId,
    String name,
    UUID tenantId
) {}
