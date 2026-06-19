plugins {
    `java-library`
}

dependencies {
    compileOnly(libs.jakarta.persistence.api)
    compileOnly(libs.hibernate.core)
}
