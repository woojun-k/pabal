plugins {
    id("pabal.java-library-conventions")
}

dependencies {
    implementation(project(":pabal-common"))

    implementation(libs.jackson.annotations)
    implementation(libs.opentelemetry.api)
    implementation(libs.slf4j.api)
    implementation(libs.spring.context)
    implementation(libs.spring.orm)
    implementation(libs.spring.security.core)
    implementation(libs.spring.tx)
    implementation(libs.spring.web)
    implementation(libs.spring.webmvc)

    compileOnly(libs.jakarta.servlet.api)
    compileOnly(libs.jakarta.validation.api)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.spring.boot.starter.validation.test)
}
