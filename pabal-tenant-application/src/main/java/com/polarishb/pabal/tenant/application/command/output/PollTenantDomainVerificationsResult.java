package com.polarishb.pabal.tenant.application.command.output;

public record PollTenantDomainVerificationsResult(
        int queuedCount,
        int verifiedCount,
        int failedCount,
        int expiredCount,
        int skippedCount
) {
}
