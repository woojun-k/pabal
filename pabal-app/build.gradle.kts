import org.springframework.boot.gradle.tasks.run.BootRun

tasks.named<BootRun>("bootRun") {
    workingDir = rootProject.projectDir
}

plugins {
    alias(libs.plugins.spring.boot)
    id("pabal.java-conventions")
}

dependencies {
    developmentOnly(platform(libs.spring.boot.dependencies))

    implementation(project(":pabal-common"))
    implementation(project(":pabal-web"))
    implementation(project(":pabal-security"))
    implementation(project(":pabal-authorization"))
    implementation(project(":pabal-infra-redis"))
    implementation(project(":pabal-tenant-api"))
    implementation(project(":pabal-tenant-application"))
    implementation(project(":pabal-tenant-infrastructure"))
    implementation(project(":pabal-workspace-api"))
    implementation(project(":pabal-workspace-application"))
    implementation(project(":pabal-workspace-infrastructure"))
    implementation(project(":pabal-user-api"))
    implementation(project(":pabal-user-application"))
    implementation(project(":pabal-user-infrastructure"))
    implementation(project(":pabal-messenger-api"))
    implementation(project(":pabal-messenger-application"))
    implementation(project(":pabal-messenger-infrastructure"))

    developmentOnly(libs.spring.boot.devtools)
    developmentOnly(libs.spring.boot.docker.compose)
    developmentOnly(libs.netty.resolver.dns.native.macos) {
        artifact {
            classifier = "osx-aarch_64"
        }
    }

    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.opentelemetry)

    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.database.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.websocket.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.spring.tx)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.archunit.junit5)
}
