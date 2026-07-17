package com.polarishb.pabal.tenant.application.service;

import com.polarishb.pabal.integration.contract.TenantContract;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantContractService implements TenantContract {

    private final TenantRepository tenantRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveTenant(UUID tenantId) {
        return tenantRepository.existsActiveById(tenantId);
    }
}
