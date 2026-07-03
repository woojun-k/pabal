import org.gradle.api.artifacts.ProjectDependency

plugins {
    base
    alias(libs.plugins.spring.boot) apply false
}

val boundedContexts = listOf("tenant", "workspace", "user", "messenger")
val productionProjectDependencyConfigurations = setOf(
    "api",
    "implementation",
    "compileOnly",
    "runtimeOnly",
)

fun pabalModule(context: String, layer: String): String = ":pabal-$context-$layer"

val allowedProjectDependencies = buildMap {
    put(":pabal-app", buildSet {
        add(":pabal-common")
        add(":pabal-web")
        add(":pabal-security")
        add(":pabal-authorization")
        add(":pabal-infra-redis")
        boundedContexts.forEach { context ->
            add(pabalModule(context, "api"))
            add(pabalModule(context, "application"))
            add(pabalModule(context, "infrastructure"))
        }
    })
    put(":pabal-common", emptySet())
    put(":pabal-web", setOf(":pabal-common"))
    put(":pabal-security", setOf(":pabal-common", ":pabal-authorization", ":pabal-infra-redis"))
    put(":pabal-authorization", setOf(":pabal-common", ":pabal-infra-redis"))
    put(":pabal-infra-redis", emptySet())
    put(":pabal-persistence-support", setOf(":pabal-common"))

    boundedContexts.forEach { context ->
        put(pabalModule(context, "domain"), setOf(":pabal-common"))
        put(pabalModule(context, "contract"), setOf(pabalModule(context, "domain")))
        put(
            pabalModule(context, "application"),
            setOf(pabalModule(context, "domain"), pabalModule(context, "contract"), ":pabal-common")
        )
        put(
            pabalModule(context, "api"),
            setOf(pabalModule(context, "application"), ":pabal-security", ":pabal-common")
        )
        put(
            pabalModule(context, "infrastructure"),
            setOf(
                pabalModule(context, "application"),
                pabalModule(context, "domain"),
                pabalModule(context, "contract"),
                ":pabal-persistence-support",
                ":pabal-common",
            )
        )
    }

    put(
        ":pabal-messenger-infrastructure",
        getValue(":pabal-messenger-infrastructure") + setOf(
            ":pabal-security",
            ":pabal-authorization",
        )
    )
}

val checkProjectDependencyBoundaries = tasks.register("checkProjectDependencyBoundaries") {
    group = "verification"
    description = "Checks production Gradle project dependencies against Pabal module boundaries."

    doLast {
        val violations = mutableListOf<String>()

        subprojects.forEach { subproject ->
            val allowedDependencies = allowedProjectDependencies[subproject.path].orEmpty()

            productionProjectDependencyConfigurations.forEach { configurationName ->
                val configuration = subproject.configurations.findByName(configurationName) ?: return@forEach
                configuration.dependencies
                    .withType(ProjectDependency::class.java)
                    .forEach { dependency ->
                        val dependencyPath = dependency.path
                        if (dependencyPath !in allowedDependencies) {
                            violations += "${subproject.path}:$configurationName -> $dependencyPath"
                        }
                    }
            }
        }

        check(violations.isEmpty()) {
            "Project dependency boundary violations:\n" + violations.joinToString(separator = "\n")
        }
    }
}

tasks.named("check") {
    dependsOn(checkProjectDependencyBoundaries)
}

allprojects {
    group = "com.polarishb"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withType<JavaPlugin> {
        tasks.named("check") {
            dependsOn(checkProjectDependencyBoundaries)
        }
    }
}
