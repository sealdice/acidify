@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    androidNativeArm64()

    sourceSets {
        androidNativeMain.dependencies {
            implementation(libs.ktor.client.core)
        }
    }

    targets.withType<KotlinNativeTarget> {
        val main by compilations.getting
        val nativeInterop by main.cinterops.creating {
            definitionFile.set(file("src/cinterop/curl.def"))
            includeDirs.allHeaders(file("src/cinterop/include"))
            extraOpts("-libraryPath", file("src/cinterop/lib"))
        }
    }

    explicitApi()
}
