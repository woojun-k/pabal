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
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.security.test)
}
