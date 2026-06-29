plugins {
    `java-library`
}

dependencies {
    implementation(project(":pabal-common"))

    compileOnly(libs.jakarta.persistence.api)
    compileOnly(libs.hibernate.core)
}
