plugins {
    kotlin("jvm")
    application
    id("com.google.cloud.tools.jib")
}

val ktorVersion: String by System.getProperties()
val imagePrefix: String by System.getProperties()

dependencies {
    implementation(project(":common"))
    implementation(project(":backend"))
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.anarres:parallelgzip:1.0.5")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.0")
    implementation("software.amazon.awssdk:s3:2.20.68")
    implementation("software.amazon.awssdk:sso:2.20.69")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
}

kotlin {
    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
            }
        }
    }
}

application {
    mainClass.set("dev.drzepka.wikilinks.generator.MainKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

jib {
    from {
        image = "eclipse-temurin:17-jre-focal"
    }
    to {
        image = "$imagePrefix/generator"
        tags = listOf(
            project.version.toString(),
            if (project.version.toString().contains("SNAPSHOT", ignoreCase = true)) "" else "latest"
        ).filter { it.isNotBlank() }.toSet()
    }
    container {
        creationTime.set("USE_CURRENT_TIMESTAMP")
    }
}

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "dev.drzepka.wikilinks.generator.MainKt"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree)
    from(dependencies)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
