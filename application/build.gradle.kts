import com.google.cloud.tools.jib.gradle.JibTask

plugins {
    application
    id("com.google.cloud.tools.jib")
}

val kotlinVersion: String by System.getProperties()
val imagePrefix: String by System.getProperties()

enum class JibConfiguration(val baseImage: String, val classifier: String) {
    JVM("openjdk:11.0.15-jre-slim-buster", "jvm"),
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
    implementation(project(":backend"))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    backendJvmRuntimeClasspath(project(mapOf("path" to ":backend", "configuration" to "exposedJvmRuntimeClasspath")))
}

jib {
    val entrypointFileName = "entrypoint-${jibConfiguration.classifier}.sh"

    from {
        image = jibConfiguration.baseImage
    }
    to {
        image = "$imagePrefix/application-${jibConfiguration.classifier}:${project.version}"
    }
    container {
        creationTime = "USE_CURRENT_TIMESTAMP"
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
                setFrom(project.projectDir.resolve("src"))
                into = "/app"
                includes.add(entrypointFileName)
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
        permissions = mapOf(
            "/app/$entrypointFileName" to "755"
        )
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
