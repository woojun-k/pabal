plugins {
    `java-library`
}

dependencies {
    api(project(":pabal-tenant-domain"))
    api(project(":pabal-tenant-contract"))
    implementation(project(":pabal-common"))

    implementation(libs.spring.context)
    implementation(libs.spring.tx)

    testImplementation(libs.spring.boot.starter.test)
}
