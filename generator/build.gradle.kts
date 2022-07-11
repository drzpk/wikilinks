plugins {
    kotlin("jvm")
    application
}

val ktorVersion: String by System.getProperties()

dependencies {
    implementation(project(":common"))
    implementation(project(":backend"))
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.anarres:parallelgzip:1.0.5")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
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
