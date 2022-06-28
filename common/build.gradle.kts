plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

val kotlinVersion: String by System.getProperties()
val kotlinxSerializationVersion: String by System.getProperties()
val ktorVersion: String by System.getProperties()
val coroutinesVersion: String by System.getProperties()

kotlin {
    jvm {}
    linuxX64 {}
    js {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                implementation("org.jetbrains.kotlin:atomicfu:$kotlinVersion")
                implementation("io.github.microutils:kotlin-logging:2.1.23")
                compileOnly("io.ktor:ktor-client-core:$ktorVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.ktor:ktor-client-mock:$ktorVersion")
            }
        }
    }
}
