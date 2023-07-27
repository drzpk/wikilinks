import com.google.cloud.tools.jib.gradle.JibTask

plugins {
    application
    kotlin("jvm")
    id("com.google.cloud.tools.jib")
}

val kotlinVersion: String by System.getProperties()
val imagePrefix: String by System.getProperties()
val coroutinesVersion: String by System.getProperties()
val ktorVersion: String by System.getProperties()

enum class JibConfiguration(val baseImage: String, val classifier: String) {
    JVM("eclipse-temurin:17-jre-focal", "jvm"),
    NATIVE("${System.getProperty("imagePrefix")}/app-base:1.1", "native"),
    NONE("", "")
}

val jibConfiguration = System.getProperty("jibConfiguration")?.let {
    try {
        JibConfiguration.valueOf(it)
    } catch (e: Exception) {
        null
    }
} ?: JibConfiguration.NONE

val backendJvmRuntimeClasspath: Configuration by configurations.creating {}

dependencies {
    implementation(project(":common"))
    implementation(project(":backend"))
    backendJvmRuntimeClasspath(project(mapOf("path" to ":backend", "configuration" to "exposedJvmRuntimeClasspath")))

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.testcontainers:testcontainers:1.18.0")
    testImplementation("org.testcontainers:mockserver:1.17.3")
    testImplementation("org.mock-server:mockserver-client-java:5.15.0")
    testImplementation("org.testcontainers:junit-jupiter:1.17.3")
    testImplementation("org.apache.logging.log4j:log4j-core:2.18.0")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:2.18.0")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    testImplementation("io.ktor:ktor-client-core:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
}

jib {
    val entrypointFileName = "entrypoint-${jibConfiguration.classifier}.sh"

    from {
        image = jibConfiguration.baseImage
    }
    to {
        image = "$imagePrefix/application-${jibConfiguration.classifier}"
        tags = listOf(
            project.version.toString(),
            if (project.version.toString().contains("SNAPSHOT", ignoreCase = true)) "" else "latest"
        ).filter { it.isNotBlank() }.toSet()
    }
    container {
        creationTime.set("USE_CURRENT_TIMESTAMP")
        workingDirectory = "/app"
        entrypoint = listOf("/app/$entrypointFileName")

        if (jibConfiguration == JibConfiguration.JVM) {
            mainClass = "dev.drzepka.wikilinks.app.JvmMainKt"
            configurationName.set("backendJvmRuntimeClasspath")
        }
    }
    extraDirectories {
        paths {
            path {
                setFrom(project.projectDir.resolve("src/docker"))
                into = "/app"
                includes.add(entrypointFileName)
            }

            if (jibConfiguration == JibConfiguration.JVM) {
                path {
                    setFrom(project(":backend").buildDir.resolve("processedResources/jvm/main"))
                    into = "/app/resources"
                }
            }

            path {
                setFrom(project.buildDir.resolve("frontend"))
                into = "/app/frontend"
            }

            if (jibConfiguration == JibConfiguration.NATIVE) {
                path {
                    setFrom(project(":backend").buildDir.resolve("bin/linuxX64/releaseExecutable"))
                    into = "/app"
                    includes.add("backend.kexe")
                }
            } else if (jibConfiguration == JibConfiguration.JVM) {
                path {
                    setFrom(project(":backend").buildDir.resolve("classes/kotlin/jvm/main"))
                    into = "/app/classes"
                }
            }
        }
        permissions.apply {
            put("/app/$entrypointFileName", "755")
        }
    }
}

val unzipFrontend by tasks.registering(Copy::class) {
    dependsOn(":frontend:zip")
    val frontend = project(":frontend")
    val frontendZipFile = frontend.buildDir.resolve("libs/frontend-${frontend.version}.zip")
    from(zipTree(frontendZipFile))
    into(project(":application").buildDir.resolve("frontend"))
}

tasks.withType<JibTask> {
    doFirst {
        if (jibConfiguration == JibConfiguration.NONE)
            throw Exception("Jib configuration wasn't specified")
    }

    val dependencies = mutableListOf<Any?>(unzipFrontend)
    if (jibConfiguration == JibConfiguration.NATIVE)
        dependencies.add(tasks.findByPath(":backend:linkReleaseExecutableLinuxX64"))
    else if (jibConfiguration == JibConfiguration.JVM)
        dependencies.add(tasks.findByPath(":backend:jvmJar"))

    dependsOn(*dependencies.toTypedArray())
}

tasks.withType<Test> {
    useJUnitPlatform {
        systemProperty("imagePrefix", imagePrefix)
        systemProperty("imageVersion", project.version)
    }
}
