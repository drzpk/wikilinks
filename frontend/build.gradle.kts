import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnResolution
import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn


plugins {
    kotlin("plugin.serialization")
    kotlin("js")
    id("io.kvision")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val kotlinVersion: String by System.getProperties()
val kvisionVersion: String by System.getProperties()
val ktorVersion: String by System.getProperties()
val coroutinesVersion: String by System.getProperties()
val kotlinxSerializationVersion: String by System.getProperties()
val webDir = file("src/main/web")

kotlin {
    js {
        browser {
            // https://webpack.js.org/configuration
            runTask {
                outputFileName = "main.bundle.js"
                sourceMaps = false
                devServer = KotlinWebpackConfig.DevServer(
                    open = false,
                    port = 3000,
                    proxy = mutableMapOf(
                        "/api" to "http://localhost:8080",
                        "/kvws/*" to mapOf("target" to "ws://localhost:8080", "ws" to true)
                    ),
                    static = mutableListOf("$buildDir/processedResources/js/main")
                )
            }
            webpackTask {
                outputFileName = "main.bundle.js"
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()

        yarn.resolutions.add(YarnResolution("**/terser").apply { include(">=5.14.2") }) // Dependabot alert #2
    }
    sourceSets["main"].dependencies {
        implementation(project(":common"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
        implementation("org.jetbrains.kotlin:atomicfu:$kotlinVersion")
        implementation("io.kvision:kvision:$kvisionVersion")
        implementation("io.kvision:kvision-state:$kvisionVersion")
        implementation("io.kvision:kvision-bootstrap:$kvisionVersion")
        implementation("io.kvision:kvision-bootstrap-css:$kvisionVersion")
        implementation("io.kvision:kvision-i18n:$kvisionVersion")
        implementation("io.ktor:ktor-client-core-js:$ktorVersion")
        implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
        implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

        implementation(devNpm("flag-icons", "^6.6.3"))
        implementation(devNpm("sass-loader", "^13.0.2"))
        implementation(devNpm("sass", "^1.54.3"))
    }
    sourceSets["test"].dependencies {
        implementation(kotlin("test-js"))
        implementation("io.kvision:kvision-testutils:$kvisionVersion")
    }
    sourceSets["main"].resources.srcDir(webDir)
}
