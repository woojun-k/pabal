plugins {
    alias(libs.plugins.spring.boot)
    java
}

dependencies {
    developmentOnly(platform(libs.spring.boot.dependencies))

    implementation(project(":pabal-common"))
    implementation(project(":pabal-security"))
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
    testImplementation(libs.spring.tx)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
}
