package com.polarishb.pabal.tenant.domain.model.vo;

import com.polarishb.pabal.common.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantDomainNameTest {

    @Test
    void constructor_normalizes_domain_name() {
        TenantDomainName domainName = new TenantDomainName(" Example.COM. ");

        assertThat(domainName.value()).isEqualTo("example.com");
    }

    @Test
    void constructor_rejects_single_label_domain() {
        assertThatThrownBy(() -> new TenantDomainName("localhost"))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void constructor_rejects_url_like_value() {
        assertThatThrownBy(() -> new TenantDomainName("https://example.com"))
                .isInstanceOf(InvalidInputException.class);
    }
}
