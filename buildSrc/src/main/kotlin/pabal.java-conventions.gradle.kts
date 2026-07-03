plugins {
    java
}

val libs = the<VersionCatalogsExtension>().named("libs")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(
            libs.findVersion("java").get().requiredVersion.toInt()
        )
    }
}

val mockitoAgent = configurations.create("mockitoAgent") {
    isCanBeConsumed = false
    isCanBeResolved = true
    description = "Mockito Java agent for inline mock maker"
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    implementation(platform(libs.findLibrary("spring-boot-dependencies").get()))
    compileOnly(platform(libs.findLibrary("spring-boot-dependencies").get()))
    annotationProcessor(platform(libs.findLibrary("spring-boot-dependencies").get()))
    testImplementation(platform(libs.findLibrary("spring-boot-dependencies").get()))
    testCompileOnly(platform(libs.findLibrary("spring-boot-dependencies").get()))
    testAnnotationProcessor(platform(libs.findLibrary("spring-boot-dependencies").get()))
    testRuntimeOnly(platform(libs.findLibrary("spring-boot-dependencies").get()))

    compileOnly(libs.findLibrary("lombok").get())
    annotationProcessor(libs.findLibrary("lombok").get())
    testCompileOnly(libs.findLibrary("lombok").get())
    testAnnotationProcessor(libs.findLibrary("lombok").get())

    testImplementation(libs.findLibrary("junit-jupiter").get())
    testImplementation(libs.findLibrary("assertj-core").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())

    "mockitoAgent"(platform(libs.findLibrary("spring-boot-dependencies").get()))
    "mockitoAgent"("org.mockito:mockito-core") {
        isTransitive = false
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("-javaagent:${mockitoAgent.singleFile.absolutePath}")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}
