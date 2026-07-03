plugins {
    id("pabal.java-library-conventions")
}

dependencies {
    api(project(":pabal-common"))

    implementation(libs.spring.context)
    implementation(project(":pabal-infra-redis"))
    implementation(libs.spring.jdbc)

    testImplementation(libs.spring.boot.starter.test)
}
