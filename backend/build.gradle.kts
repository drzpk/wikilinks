plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.squareup.sqldelight")
}

val coroutinesVersion: String by System.getProperties()
val ktorVersion: String by System.getProperties()
val koinVersion: String by System.getProperties()
val atomicfuVersion: String by System.getProperties()

kotlin {
    /* Targets configuration omitted. 
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */
    jvm {
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
    }
    linuxX64 {
        binaries {
            executable {
                entryPoint = "dev.drzepka.wikilinks.app.linuxMain"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":common"))
                implementation(kotlin("stdlib-common"))
                implementation("io.github.microutils:kotlin-logging:2.1.23")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
                implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("io.insert-koin:koin-core:$koinVersion")
                implementation("org.jetbrains.kotlinx:atomicfu:$atomicfuVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val linuxX64Main by getting {
            dependencies {
                implementation("com.squareup.sqldelight:native-driver:1.5.5")
                implementation("io.github.microutils:kotlin-logging-linuxx64:2.1.23")
                implementation("io.ktor:ktor-client-curl:$ktorVersion")
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-cio:$ktorVersion")
            }
        }

        val jvmMain by getting {
            dependencies {
                api("com.squareup.sqldelight:sqlite-driver:1.5.5")
                implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")
                implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")
                implementation("io.ktor:ktor-client-apache:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("io.ktor:ktor-server-compression:$ktorVersion")
            }
        }

        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
            }
        }

        targets.named("linuxX64") {
            compilations.all {
                val libraryDir = File(projectDir, "lib")
                kotlinOptions.freeCompilerArgs += listOf("-linker-options", "-L$libraryDir")
            }
        }
    }
}

val exposedJvmRuntimeClasspath: Configuration by configurations.creating {
    // Configuration must be exposed to be used in another modules
    isCanBeConsumed = true
    isCanBeResolved = false
    extendsFrom(configurations["jvmRuntimeClasspath"])
}

sqldelight {
    database("LinksDatabase") {
        packageName = "dev.drzepka.wikilinks.db.links"
        sourceFolders = listOf("sqldelight/links")
        schemaOutputDirectory = file("build/dbs")
    }
    database("CacheDatabase") {
        packageName = "dev.drzepka.wikilinks.db.cache"
        sourceFolders = listOf("sqldelight/cache")
        schemaOutputDirectory = file("build/dbs")
    }
    database("HistoryDatabase") {
        packageName = "dev.drzepka.wikilinks.db.history"
        sourceFolders = listOf("sqldelight/history")
        schemaOutputDirectory = file("build/dbs")
    }

    linkSqlite = true
}
