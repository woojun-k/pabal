plugins {
    `java-library`
}

dependencies {
    api(project(":pabal-common"))
    implementation(project(":pabal-authorization"))
    implementation(project(":pabal-infra-redis"))

    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.webmvc)
    // Intentional: refresh token rotation is security infrastructure; RBAC JDBC lives in pabal-authorization.
    implementation(libs.spring.jdbc)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.security.test)
}
