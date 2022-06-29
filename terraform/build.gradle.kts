tasks.register<Exec>("uploadFrontend") {
    dependsOn(getBucketName, tasks.findByPath(":frontend:browserProductionWebpack"))

    doFirst {
        val name = getBucketName.get().extra["bucketName"]
        println("Uploading frontend distribution to bucket $name")
        commandLine = listOf("aws", "s3", "sync", ".", "s3://$name/distribution")
    }

    workingDir = project(":frontend").buildDir.resolve("distributions")
}

val getBucketName by tasks.registering(Exec::class) {
    workingDir = projectDir.resolve("src")
    commandLine = listOf("terraform", "output", "-raw", "bucket_name")
    standardOutput = java.io.ByteArrayOutputStream()

    doLast {
        extra["bucketName"] = standardOutput.toString()
    }
}
