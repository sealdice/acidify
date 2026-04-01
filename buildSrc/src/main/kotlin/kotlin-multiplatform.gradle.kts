package buildsrc.convention

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    val actionsTarget = System.getenv("GITHUB_ACTIONS_BUILD_TARGET")
    when (actionsTarget) {
        // is run from GitHub Actions - build Yogurt, improving dependency pulling time
        "jvm" -> {} // already added
        "androidNativeArm64" -> androidNativeArm64()
        "mingwX64" -> mingwX64()
        "macosArm64" -> macosArm64()
        "linuxX64" -> linuxX64()
        "linuxArm64" -> linuxArm64()

        // is run locally
        else -> {
            jvm()
            mingwX64()
            macosArm64()
            linuxX64()
            linuxArm64()
        }
    }

    jvmToolchain(25)
}
