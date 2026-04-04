import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

group = "org.ntqqrev.local"
version = "0.0.0"

repositories {
    mavenCentral()
}

kotlin {
    androidNativeArm64()

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.9.0")
        }
    }

    targets.withType<KotlinNativeTarget> {
        val main by compilations.getting
        val nativeInterop by main.cinterops.creating {
            definitionFile.set(project.file("src/nativeInterop/interop.def"))
            extraOpts("-libraryPath", project.file("src/nativeInterop/lib/androidArm64").absolutePath)
        }
    }
}
