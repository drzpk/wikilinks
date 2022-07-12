import com.google.cloud.tools.jib.gradle.JibTask

plugins {
    application
    id("com.google.cloud.tools.jib")
}

val imagePrefix: String by System.getProperties()

jib {
    from {
        image = "$imagePrefix/app-base:1.0"
    }
    to {
        image = "$imagePrefix/application:${project.version}"
    }
    container {
        creationTime = "USE_CURRENT_TIMESTAMP"
        workingDirectory = "/app"
        entrypoint = listOf("/app/entrypoint.sh")
    }
    extraDirectories {
        paths {
            path {
                setFrom(project.projectDir.resolve("src"))
                into = "/app"
                includes.add("entrypoint.sh")
            }
            path {
                setFrom(project(":backend").buildDir.resolve("bin/linuxX64/releaseExecutable"))
                into = "/app"
                includes.add("backend.kexe")
            }
            path {
                setFrom(project(":frontend").buildDir.resolve("libs"))
                into = "/app"
                includes.add("frontend*.zip")
            }
        }
        permissions = mapOf(
            "/app/entrypoint.sh" to "755"
        )
    }
}

tasks.withType<JibTask> {
    dependsOn(
        tasks.findByPath(":backend:linkReleaseExecutableLinuxX64"),
        tasks.findByPath(":frontend:zip")
    )
}
