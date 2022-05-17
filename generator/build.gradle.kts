plugins {
    kotlin("jvm") version "1.6.10"
    id("com.squareup.sqldelight")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

sqldelight {
    database("Database") {
        dependency(project(":application"))
        packageName = "dev.drzepka.wikilinks.db"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}