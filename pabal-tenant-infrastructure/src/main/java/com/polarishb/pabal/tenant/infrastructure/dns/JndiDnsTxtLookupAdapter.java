package com.polarishb.pabal.tenant.infrastructure.dns;

import com.polarishb.pabal.tenant.application.port.out.dns.DnsTxtLookupPort;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class JndiDnsTxtLookupAdapter implements DnsTxtLookupPort {

    @Override
    public Set<String> lookupTxtRecords(String dnsName) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

        InitialDirContext context = null;
        try {
            context = new InitialDirContext(env);
            Attributes attributes = context.getAttributes(dnsName, new String[]{"TXT"});
            Attribute attribute = attributes.get("TXT");
            if (attribute == null) {
                return Set.of();
            }

            Set<String> values = new LinkedHashSet<>();
            NamingEnumeration<?> records = attribute.getAll();
            while (records.hasMore()) {
                values.add(normalizeTxtValue(records.next()));
            }
            return values;
        } catch (NamingException e) {
            return Set.of();
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException ignored) {
                    // DNS lookup already completed; close failures do not change verification result.
                }
            }
        }
    }

    private String normalizeTxtValue(Object record) {
        if (record == null) {
            return "";
        }
        String value = record.toString().trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
