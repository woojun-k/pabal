plugins {
    java
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.polarishb"
version = "0.0.1-SNAPSHOT"
description = "pabal"

val testcontainersVersion = "1.21.3"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // =====================================================
    // 개발 편의 / 로컬 개발 환경 전용
    // - 운영 배포에는 직접 포함되지 않는 개발용 의존성
    // - 추후 "local-dev", "dev-support" 성격으로 분리 가능
    // =====================================================
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // Apple Silicon(macOS aarch64) 환경에서 Netty DNS resolver 보완
    // - 로컬 개발 환경 이슈 대응용
    // - OS/플랫폼 종속 항목이므로 별도 관리하기 쉽게 분리
    developmentOnly("io.netty:netty-resolver-dns-native-macos::osx-aarch_64")

    // =====================================================
    // 공통 웹 인터페이스
    // - HTTP API, Reactive API, WebSocket 엔드포인트
    // - 추후 api / realtime 모듈 분리 시 기준점이 되는 영역
    // =====================================================
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // =====================================================
    // 보안 / 인증 / 인가
    // - 인증 필터, 인가, 메시징 보안, Resource Server
    // - 추후 security 또는 auth-support 모듈로 분리 가능
    // =====================================================
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-messaging")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // =====================================================
    // 입력 검증
    // - Request DTO validation, Bean Validation
    // - 웹 계층과 함께 가거나 별도 validation-support 로 분리 가능
    // =====================================================
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // =====================================================
    // 데이터 접근 / 영속성
    // - RDB 영속성, Redis 동기/반응형 접근
    // - 추후 persistence / cache-access 계층으로 나누기 쉬운 영역
    // =====================================================
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")

    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // =====================================================
    // 메시징 / 이벤트 스트리밍
    // - Kafka 기반 이벤트 발행/구독
    // - 추후 messaging / eventing 모듈로 분리 가능
    // =====================================================
    implementation("org.springframework.boot:spring-boot-starter-kafka")

    // =====================================================
    // 운영 / 관측성
    // - 헬스체크, 메트릭, 트레이싱 연계
    // - 추후 observability / monitoring 모듈로 분리 가능
    // =====================================================
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")

    // =====================================================
    // 런타임 DB 드라이버
    // - 운영/로컬 환경별 드라이버
    // - 추후 profile 또는 infra-runtime 의존성으로 분리 가능
    // =====================================================
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    // =====================================================
    // 코드 생성 / 컴파일 보조
    // - Lombok은 전역 공통으로 분리하기 쉬운 대표 항목
    // =====================================================
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // =====================================================
    // 테스트 공통
    // - 대부분의 테스트 기반 의존성
    // - 추후 test-common / integration-test-support 로 분리 가능
    // =====================================================
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(platform("org.testcontainers:testcontainers-bom:$testcontainersVersion"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:testcontainers")

    // =====================================================
    // 테스트: Spring 기능별 슬라이스 / 지원 라이브러리
    // - 특정 기술 스택별 테스트를 명시적으로 분리
    // - 추후 web-test / data-test / messaging-test 식 분리 가능
    // =====================================================
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")

    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-reactive-test")

    testImplementation("org.springframework.boot:spring-boot-starter-kafka-test")

    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("org.springframework.boot:spring-boot-starter-websocket-test")

    // =====================================================
    // 테스트 런타임 전용
    // - 테스트 실행 시에만 필요한 런타임 구성요소
    // =====================================================
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
