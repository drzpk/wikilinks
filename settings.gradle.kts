rootProject.name = "wikilinks"

include("common", "application", "frontend", "generator", "updater", "terraform")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.squareup.sqldelight") {
                useModule("com.squareup.sqldelight:gradle-plugin:1.5.3")
            }
        }
    }
}
