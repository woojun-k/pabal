package com.polarishb.pabal.tenant.contract.persistence;

import com.polarishb.pabal.tenant.domain.model.TenantRegistration;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TenantRegistrationPersistenceBoundaryTest {

    private static final String TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEF";
    private static final Instant CREATED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final UUID REGISTRATION_ID =
            UUID.fromString("018f57d7-0000-7000-8000-000000000001");
    private static final Pattern STATE_VARIABLE_DECLARATION =
            Pattern.compile("\\bTenantRegistrationState\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b");
    private static final List<String> STATE_FORMATTING_HELPERS = List.of(
            "verificationDnsName",
            "verificationTxtValue"
    );

    @Test
    void tenant_registration_state_does_not_expose_verification_formatting_helpers() {
        List<String> declaredMethodNames = Arrays.stream(TenantRegistrationState.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();

        assertThat(declaredMethodNames)
                .doesNotContain("verificationDnsName", "verificationTxtValue");
    }

    @Test
    void tenant_registration_state_does_not_contain_verification_dns_or_txt_prefixes() throws Exception {
        List<String> staticStringFieldValues = staticStringFieldValues(TenantRegistrationState.class);
        String source = Files.readString(repositoryRoot().resolve(
                "pabal-tenant-contract/src/main/java/com/polarishb/pabal/tenant/contract/persistence/TenantRegistrationState.java"
        ));

        assertThat(staticStringFieldValues)
                .doesNotContain("_pabal-verification.", "pabal-verification=");
        assertThat(source)
                .doesNotContain("_pabal-verification.", "pabal-verification=");
    }

    @Test
    void production_sources_do_not_call_verification_formatting_helpers_on_tenant_registration_state()
            throws IOException {
        List<String> violations = new ArrayList<>();
        Path root = repositoryRoot();

        for (Path sourcePath : tenantProductionSources(root)) {
            String source = Files.readString(sourcePath);
            Matcher declarationMatcher = STATE_VARIABLE_DECLARATION.matcher(source);
            while (declarationMatcher.find()) {
                String stateVariable = declarationMatcher.group(1);
                collectStateFormattingHelperCalls(root, sourcePath, source, stateVariable, violations);
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void mapper_reconstitutes_domain_verification_values_from_state() {
        TenantRegistrationState state = new TenantRegistrationState(
                REGISTRATION_ID,
                "Acme",
                "Example.COM.",
                TOKEN,
                TenantRegistrationStatus.PENDING_VERIFICATION,
                CREATED_AT.plusSeconds(3600),
                null,
                null,
                null,
                null,
                CREATED_AT,
                CREATED_AT,
                0L
        );

        TenantRegistration registration = TenantRegistrationPersistenceMapper.toPersisted(state).registration();

        assertThat(registration.verificationDnsName()).isEqualTo("_pabal-verification.example.com");
        assertThat(registration.verificationTxtValue()).isEqualTo("pabal-verification=" + TOKEN);
    }

    private static void collectStateFormattingHelperCalls(
            Path root,
            Path sourcePath,
            String source,
            String stateVariable,
            List<String> violations
    ) {
        for (String helper : STATE_FORMATTING_HELPERS) {
            Pattern helperCall = Pattern.compile("\\b" + Pattern.quote(stateVariable)
                    + "\\s*\\.\\s*" + helper + "\\s*\\(");
            Matcher helperMatcher = helperCall.matcher(source);
            while (helperMatcher.find()) {
                violations.add(root.relativize(sourcePath) + ":" + lineNumber(source, helperMatcher.start()));
            }
        }
    }

    private static List<String> staticStringFieldValues(Class<?> type) throws IllegalAccessException {
        List<String> values = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(String.class)) {
                field.setAccessible(true);
                values.add((String) field.get(null));
            }
        }
        return values;
    }

    private static List<Path> tenantProductionSources(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("pabal-tenant-"))
                    .filter(path -> path.toString().contains("src/main/java"))
                    .toList();
        }
    }

    private static long lineNumber(String source, int offset) {
        return source.substring(0, offset)
                .chars()
                .filter(character -> character == '\n')
                .count() + 1;
    }

    private static Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle.kts"))
                    && Files.exists(current.resolve("pabal-tenant-contract"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("repository root not found");
    }
}
