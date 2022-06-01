plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":application"))
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.anarres:parallelgzip:1.0.5")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}