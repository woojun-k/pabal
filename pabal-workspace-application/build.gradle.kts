plugins {
    `java-library`
}

dependencies {
    api(project(":pabal-workspace-domain"))
    api(project(":pabal-workspace-contract"))
    implementation(project(":pabal-common"))

    implementation(libs.spring.context)
    implementation(libs.spring.tx)

    testImplementation(libs.spring.boot.starter.test)
}
