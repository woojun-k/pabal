plugins {
    `java-library`
}

dependencies {
    api(project(":pabal-user-domain"))
    api(project(":pabal-user-contract"))
    implementation(project(":pabal-common"))

    implementation(libs.spring.context)
    implementation(libs.spring.tx)

    testImplementation(libs.spring.boot.starter.test)
}
