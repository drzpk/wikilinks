plugins {
    val kotlinVersion: String by System.getProperties()
    val kvisionVersion: String by System.getProperties()

    kotlin("multiplatform") version "1.8.22" apply false
    kotlin("jvm") version kotlinVersion  apply false
    kotlin("js") version kotlinVersion  apply false
    kotlin("plugin.serialization") version kotlinVersion  apply false
    id("io.kvision") version kvisionVersion apply false
    id("com.google.cloud.tools.jib") version "3.3.2" apply false
}

group = "dev.drzepka.wikilinks"
version = "1.3.0"

allprojects {
    tasks.register("printConfigurations") {
        doLast {
            println("Configurations of project ${project.name}:")
            configurations.forEach {
                println("    ${it.name}")
            }
        }
    }
}

allprojects {
    version = rootProject.version
    repositories {
        mavenCentral()
    }
}
