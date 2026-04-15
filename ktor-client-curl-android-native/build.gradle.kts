@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.ensody.nativebuilds.cinterops
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.nativebuilds)
}

kotlin {
    androidNativeArm64()

    sourceSets {
        androidNativeMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.bundles.nativebuilds.curl)
        }
    }

    cinterops(libs.nativebuilds.curl.headers) {
        definitionFile.set(file("src/androidNativeArm64Main/cinterop/curl.def"))
    }

    explicitApi()
}
