plugins {
    val kotlinVersion: String by System.getProperties()
    val kvisionVersion: String by System.getProperties()

    kotlin("multiplatform") version "1.6.20" apply false
    kotlin("jvm") version kotlinVersion  apply false
    kotlin("js") version kotlinVersion  apply false
    kotlin("plugin.serialization") version kotlinVersion  apply false
    id("io.kvision") version kvisionVersion apply false
    id("com.google.cloud.tools.jib") version "3.2.1" apply false
}

group = "dev.drzepka"
version = "1.0-SNAPSHOT"

subprojects {
    version = rootProject.version
    repositories {
        mavenCentral()
    }
}