package com.polarishb.pabal.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 금지 의존의 근거는 docs/architecture/package-structure-and-layers.md의
 * "허용 의존"/"금지 의존" 목록과 ADR-0009(security의 spring-messaging 금지),
 * ADR-0012(persistence-support 분리)다. Gradle 모듈 경계 검사가 module 단위를
 * 막고, 이 테스트는 package 단위와 라이브러리 의존(JPA, spring-messaging,
 * ThreadLocal)까지 막는다.
 */
@AnalyzeClasses(
        packages = "com.polarishb.pabal",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class PabalArchitectureTest {

    private static final List<String> BOUNDED_CONTEXTS = List.of("tenant", "workspace", "user", "messenger");

    private static final String[] BOUNDED_CONTEXT_PACKAGES = BOUNDED_CONTEXTS.stream()
            .map(context -> "com.polarishb.pabal." + context + "..")
            .toArray(String[]::new);

    private static final String PERSISTENCE_SUPPORT_PACKAGE = "com.polarishb.pabal.persistence..";

    private static final String[] INFRASTRUCTURE_PACKAGES =
            concat(contextLayerPackages("infrastructure"), "com.polarishb.pabal.infrastructure..");

    private static String[] contextLayerPackages(String... layers) {
        return Arrays.stream(layers)
                .flatMap(layer -> BOUNDED_CONTEXTS.stream()
                        .map(context -> "com.polarishb.pabal." + context + "." + layer + ".."))
                .toArray(String[]::new);
    }

    private static String[] concat(String[] packages, String... more) {
        return Stream.concat(Arrays.stream(packages), Arrays.stream(more)).toArray(String[]::new);
    }

    @ArchTest
    static final ArchRule domain_must_not_depend_on_outer_layers =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..contract..",
                            "..application..",
                            "..api..",
                            "..infrastructure..",
                            "..security.."
                    );

    @ArchTest
    static final ArchRule domain_must_stay_framework_free =
            noClasses()
                    .that().resideInAnyPackage(contextLayerPackages("domain"))
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.."
                    )
                    .because("domain은 HTTP/STOMP/JPA 등 framework에 의존하지 않는다");

    @ArchTest
    static final ArchRule application_must_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAnyPackage(contextLayerPackages("application"))
                    .should().dependOnClassesThat().resideInAnyPackage(INFRASTRUCTURE_PACKAGES)
                    .because("금지 의존: {bounded-context}-application → {bounded-context}-infrastructure");

    @ArchTest
    static final ArchRule application_must_not_depend_on_api =
            noClasses()
                    .that().resideInAnyPackage(contextLayerPackages("application"))
                    .should().dependOnClassesThat().resideInAnyPackage(contextLayerPackages("api"))
                    .because("허용 의존은 application → domain/contract/common 뿐이다");

    @ArchTest
    static final ArchRule api_must_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAnyPackage(contextLayerPackages("api"))
                    .should().dependOnClassesThat().resideInAnyPackage(INFRASTRUCTURE_PACKAGES)
                    .because("금지 의존: {bounded-context}-api → {bounded-context}-infrastructure");

    @ArchTest
    static final ArchRule contract_must_not_depend_on_outer_layers =
            noClasses()
                    .that().resideInAnyPackage(contextLayerPackages("contract"))
                    .should().dependOnClassesThat().resideInAnyPackage(
                            concat(
                                    contextLayerPackages("application", "api"),
                                    concat(INFRASTRUCTURE_PACKAGES, "com.polarishb.pabal.security..")
                            )
                    )
                    .because("허용 의존은 {bounded-context}-contract → {bounded-context}-domain/common 뿐이다");

    @ArchTest
    static final ArchRule inner_layers_must_not_depend_on_persistence_support =
            noClasses()
                    .that().resideInAnyPackage(contextLayerPackages("domain", "contract", "application", "api"))
                    .should().dependOnClassesThat().resideInAPackage(PERSISTENCE_SUPPORT_PACKAGE)
                    .because("금지 의존: {bounded-context}-domain/application/api/contract → pabal-persistence-support (ADR-0012)");

    @ArchTest
    static final ArchRule shared_modules_must_not_depend_on_bounded_contexts =
            noClasses()
                    .that().resideInAnyPackage(
                            "com.polarishb.pabal.common..",
                            "com.polarishb.pabal.web..",
                            "com.polarishb.pabal.security..",
                            "com.polarishb.pabal.authorization..",
                            PERSISTENCE_SUPPORT_PACKAGE
                    )
                    .should().dependOnClassesThat().resideInAnyPackage(BOUNDED_CONTEXT_PACKAGES)
                    .because("금지 의존: common/web/security/authorization/persistence-support → bounded context");

    @ArchTest
    static final ArchRule security_must_not_depend_on_spring_messaging =
            noClasses()
                    .that().resideInAPackage("com.polarishb.pabal.security..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.messaging..")
                    .because("금지 의존: pabal-security → spring-messaging (ADR-0009)");

    @ArchTest
    static final ArchRule jpa_must_not_leak_outside_infrastructure =
            noClasses()
                    .that().resideOutsideOfPackages(concat(INFRASTRUCTURE_PACKAGES, PERSISTENCE_SUPPORT_PACKAGE))
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                    .because("JPA entity와 jakarta.persistence는 infrastructure/persistence-support를 벗어나지 않는다");

    @ArchTest
    static final ArchRule repository_ports_must_not_live_in_domain =
            noClasses()
                    .should().resideInAPackage("..domain.repository..")
                    .because("repository port는 application.port.out.persistence에 둔다");

    @ArchTest
    static final ArchRule thread_local_is_forbidden =
            noClasses()
                    .should().dependOnClassesThat().areAssignableTo(ThreadLocal.class)
                    .because("동시성 컨텍스트는 ThreadLocal이 아닌 ScopedValue로 전달한다 (virtual thread 호환)");
}
