plugins {
    `java-library`
}

dependencies {
    api(project(":pabal-messenger-domain"))
    api(project(":pabal-messenger-contract"))
    implementation(project(":pabal-common"))

    implementation(libs.spring.context)
    implementation(libs.spring.tx)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.security.test)
}
