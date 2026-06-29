plugins {
    `java-library`
}

dependencies {
    api(libs.spring.boot.starter.data.redis)
    api(libs.spring.boot.starter.data.redis.reactive)
}
