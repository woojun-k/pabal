plugins {
    id("pabal.java-library-conventions")
}

dependencies {
    api(project(":pabal-workspace-domain"))
    api(project(":pabal-workspace-contract"))
    api(project(":pabal-common"))
    api(project(":pabal-integration-contract"))

    implementation(libs.spring.context)
    implementation(libs.spring.tx)

    testImplementation(libs.spring.boot.starter.test)
}
