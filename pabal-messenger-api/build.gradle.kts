plugins {
    `java-library`
}

dependencies {
    implementation(project(":pabal-messenger-application"))
    implementation(project(":pabal-security"))
    implementation(project(":pabal-common"))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.security.test)
}
