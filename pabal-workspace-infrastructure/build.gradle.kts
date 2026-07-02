plugins {
    `java-library`
}

dependencies {
    implementation(project(":pabal-common"))
    implementation(project(":pabal-persistence-support"))
    implementation(project(":pabal-workspace-application"))
    implementation(project(":pabal-workspace-domain"))
    implementation(project(":pabal-workspace-contract"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.flyway)

    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.database.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers)
}
