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
                outputFileName = "main.bundle.[contenthash].js"
                sourceMaps = false
            }
            webpackTask {
                outputFileName = "main.bundle.[contenthash].js"
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
        implementation("io.kvision:kvision-i18n:$kvisionVersion")
        implementation("io.kvision:kvision-routing-navigo-ng:$kvisionVersion")
        implementation("io.ktor:ktor-client-core-js:$ktorVersion")
        implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
        implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

        implementation(devNpm("bootstrap-icons", "^1.9.1"))
        implementation(devNpm("flag-icons", "^6.6.3"))
        implementation(devNpm("sass-loader", "^13.0.2"))
        implementation(devNpm("sass", "^1.54.3"))
        implementation(devNpm("html-webpack-plugin", "^5.5.0"))
    }
    sourceSets["test"].dependencies {
        implementation(kotlin("test-js"))
        implementation("io.kvision:kvision-testutils:$kvisionVersion")
    }
    sourceSets["main"].resources.srcDir(webDir)
}

afterEvaluate {
      tasks.named<Zip>("zip").get().apply {
        exclude("index.template.html")
    }
}
