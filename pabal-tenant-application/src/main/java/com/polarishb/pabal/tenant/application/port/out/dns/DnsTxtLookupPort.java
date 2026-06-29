package com.polarishb.pabal.tenant.application.port.out.dns;

import java.util.Set;

public interface DnsTxtLookupPort {
    Set<String> lookupTxtRecords(String dnsName);
}
