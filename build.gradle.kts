import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    java
    alias(libs.plugins.spring.boot) apply false
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

allprojects {
    group = "com.polarishb"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withType<JavaPlugin> {

        val mockitoAgent = configurations.create("mockitoAgent") {
            isCanBeConsumed = false
            isCanBeResolved = true
            isVisible = false
            description = "Mockito Java agent for inline mock maker"
        }

        java {
            toolchain {
                languageVersion = JavaLanguageVersion.of(
                    versionCatalog.findVersion("java").get().requiredVersion.toInt()
                )
            }
        }

        configurations {
            compileOnly {
                extendsFrom(configurations.annotationProcessor.get())
            }
        }

        dependencies {
            add("implementation", platform(versionCatalog.findLibrary("spring-boot-dependencies").get()))
            add("compileOnly", platform(versionCatalog.findLibrary("spring-boot-dependencies").get()))
            add("annotationProcessor", platform(versionCatalog.findLibrary("spring-boot-dependencies").get()))
            add("testImplementation", platform(versionCatalog.findLibrary("spring-boot-dependencies").get()))
            add("testCompileOnly", platform(versionCatalog.findLibrary("spring-boot-dependencies").get()))
            add("testAnnotationProcessor", platform(versionCatalog.findLibrary("spring-boot-dependencies").get()))
            add("testRuntimeOnly", platform(versionCatalog.findLibrary("spring-boot-dependencies").get()))

            add("compileOnly", versionCatalog.findLibrary("lombok").get())
            add("annotationProcessor", versionCatalog.findLibrary("lombok").get())
            add("testCompileOnly", versionCatalog.findLibrary("lombok").get())
            add("testAnnotationProcessor", versionCatalog.findLibrary("lombok").get())

            add("testImplementation", versionCatalog.findLibrary("junit-jupiter").get())
            add("testImplementation", versionCatalog.findLibrary("assertj-core").get())
            add("testRuntimeOnly", versionCatalog.findLibrary("junit-platform-launcher").get())

            add("mockitoAgent", platform(versionCatalog.findLibrary("spring-boot-dependencies").get()))
            add("mockitoAgent", "org.mockito:mockito-core") {
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
    }
}
