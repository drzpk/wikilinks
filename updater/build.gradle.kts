plugins {
    kotlin("jvm")
    application
}

val ktorVersion: String by System.getProperties()

dependencies {
    implementation(project(":common"))
    implementation(platform("software.amazon.awssdk:bom:2.17.220"))
    implementation("software.amazon.awssdk:ec2")
    implementation("software.amazon.awssdk:sso")
    implementation("com.amazonaws:aws-lambda-java-core:1.2.1")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.0")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("io.mockk:mockk:1.12.4")
}

val service = project.extensions.getByType<JavaToolchainService>()
val customLauncher = service.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(11))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain>().configureEach {
    kotlinJavaToolchain.toolchain.use(customLauncher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "dev.drzepka.wikilinks.updater.MainKt"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree)
    from(dependencies)
}