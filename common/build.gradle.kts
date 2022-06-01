plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {}
    linuxX64 {}
    js {
        browser()
    }

    sourceSets {
        val commonMain by getting {

        }
    }
}
