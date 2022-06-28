plugins {
    kotlin("jvm")
}

val ktorVersion: String by System.getProperties()

dependencies {
    implementation(project(":common"))
    implementation(project(":application"))
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.anarres:parallelgzip:1.0.5")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")

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

tasks.withType<Test> {
    useJUnitPlatform()
}
