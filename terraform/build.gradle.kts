import java.nio.file.Files

tasks.register<Exec>("uploadFrontend") {
    dependsOn(getBucketName, tasks.findByPath(":frontend:browserProductionWebpack"))

    doFirst {
        val name = getBucketName.get().extra["bucketName"]
        commandLine = listOf("aws", "s3", "sync", ".", "s3://$name/frontend")
    }

    workingDir = project(":frontend").buildDir.resolve("distributions")
}

tasks.register<Exec>("uploadGenerator") {
    dependsOn(getBucketName, tasks.findByPath(":generator:jar"))

    val targetDir = project(":generator").buildDir.resolve("libs")
    doFirst {
        val scriptPath = project(":terraform").projectDir.resolve("src/scripts/processed/generator.sh").toPath()
        Files.copy(scriptPath, targetDir.resolve("generator.sh").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)

        val name = getBucketName.get().extra["bucketName"]
        commandLine = listOf("aws", "s3", "sync", ".", "s3://$name/generator")
    }

    workingDir = targetDir
}

tasks.register<Exec>("uploadApplication") {
    dependsOn(getBucketName, tasks.findByPath(":backend:linkReleaseExecutableLinuxX64"))

    doFirst {
        val name = getBucketName.get().extra["bucketName"]
        commandLine = listOf("aws", "s3", "sync", ".", "s3://$name/application")
    }

    workingDir = project(":backend").buildDir.resolve("bin/linuxX64/releaseExecutable")
}

val getBucketName by tasks.registering(Exec::class) {
    workingDir = projectDir.resolve("src")
    commandLine = listOf("terraform", "output", "-raw", "bucket_name")
    standardOutput = java.io.ByteArrayOutputStream()

    doLast {
        val name = standardOutput.toString()
        println("Bucket name: $name")
        extra["bucketName"] = name
    }
}
