plugins {
    kotlin("multiplatform") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("com.squareup.sqldelight")
}

kotlin {
    /* Targets configuration omitted. 
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */
    jvm {}
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
                implementation(kotlin("stdlib-common"))
                implementation("io.github.microutils:kotlin-logging:2.1.23")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val linuxX64Main by getting {
            dependencies {
                implementation("com.squareup.sqldelight:native-driver:1.5.3")
                implementation("io.github.microutils:kotlin-logging-linuxx64:2.1.23")
            }
        }

        val jvmMain by getting {
            dependencies {
                api("com.squareup.sqldelight:sqlite-driver:1.5.3")
                implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")
                implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")
                implementation("io.ktor:ktor-server-core:1.6.5")
                implementation("io.ktor:ktor-server-netty:1.6.5")
                implementation("io.ktor:ktor-jackson:1.6.5")
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

sqldelight {
    database("Database") {
        packageName = "dev.drzepka.wikilinks.db"
        schemaOutputDirectory = file("build/dbs")
    }
    linkSqlite = true
}
