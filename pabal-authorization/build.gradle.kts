plugins {
    `java-library`
}

dependencies {
    api(project(":pabal-common"))

    api(libs.spring.context)
    implementation(project(":pabal-infra-redis"))
    implementation(libs.spring.jdbc)

    testImplementation(libs.spring.boot.starter.test)
}
