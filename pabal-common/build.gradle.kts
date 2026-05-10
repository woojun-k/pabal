plugins {
    `java-library`
}

dependencies {
    compileOnly(libs.jakarta.persistence.api)
    compileOnly(libs.jakarta.servlet.api)
    compileOnly(libs.jakarta.validation.api)
    compileOnly(libs.hibernate.core)

    implementation(libs.spring.context)
    implementation(libs.spring.tx)
    implementation(libs.spring.web)
    implementation(libs.spring.webmvc)
    implementation(libs.spring.orm)
    implementation(libs.spring.security.core)
    implementation(libs.opentelemetry.api)
    implementation(libs.jackson.annotations)
    implementation(libs.slf4j.api)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.spring.boot.starter.validation.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers)
}
