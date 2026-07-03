plugins {
    id("pabal.java-library-conventions")
}

dependencies {
    implementation(project(":pabal-common"))
    implementation(project(":pabal-security"))
    implementation(project(":pabal-workspace-application"))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.security.test)
}
