plugins {
    id("pabal.java-library-conventions")
}

dependencies {
    implementation(project(":pabal-common"))
    implementation(project(":pabal-persistence-support"))
    implementation(project(":pabal-security"))
    implementation(project(":pabal-authorization"))
    implementation(project(":pabal-messenger-application"))
    implementation(project(":pabal-messenger-domain"))
    implementation(project(":pabal-messenger-contract"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.messaging)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.micrometer.core)

    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.database.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.websocket.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers)
}
