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

    doFirst {
        val name = getBucketName.get().extra["bucketName"]
        commandLine = listOf("aws", "s3", "sync", ".", "s3://$name/generator")
    }

    workingDir = project(":generator").buildDir.resolve("libs")
}

tasks.register<Exec>("uploadApplication") {
    dependsOn(getBucketName, tasks.findByPath(":application:linkReleaseExecutableLinuxX64"))

    doFirst {
        val name = getBucketName.get().extra["bucketName"]
        commandLine = listOf("aws", "s3", "sync", ".", "s3://$name/application")
    }

    workingDir = project(":application").buildDir.resolve("bin/linuxX64/releaseExecutable")
}

tasks.register<Exec>("uploadUpdater") {
    dependsOn(getBucketName, tasks.findByPath(":updater:jar"))

    doFirst {
        val name = getBucketName.get().extra["bucketName"]
        commandLine = listOf("aws", "s3", "sync", ".", "s3://$name/updater")
    }

    workingDir = project(":updater").buildDir.resolve("libs")
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
