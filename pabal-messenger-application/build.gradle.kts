plugins {
    id("pabal.java-library-conventions")
}

dependencies {
    api(project(":pabal-common"))
    api(project(":pabal-messenger-domain"))
    api(project(":pabal-messenger-contract"))

    implementation(libs.spring.context)
    implementation(libs.spring.tx)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.security.test)
}
