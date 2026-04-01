import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.build.konfig)
    alias(libs.plugins.kotlinx.atomicfu)
}

version = "0.1.0"

kotlin {
    sourceSets {
        val commonMain = getByName("commonMain")
        commonMain.dependencies {
            implementation(project(":acidify-core"))
            implementation(libs.kotlinx.datetime)
            implementation(libs.bundles.ktor.client)
            implementation(libs.bundles.ktor.server)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.milky.types)
            implementation(libs.qr.matrix)
        }

        val codecMain by creating {
            dependsOn(commonMain)

            dependencies {
                implementation(libs.acidify.codec)
            }
        }

        val nativeMain = if (
            findByName("androidNativeArm64Main") != null ||
            findByName("linuxX64Main") != null ||
            findByName("linuxArm64Main") != null ||
            findByName("macosArm64Main") != null ||
            findByName("mingwX64Main") != null
        ) {
            maybeCreate("nativeMain").apply {
                dependsOn(commonMain)
            }
        } else {
            null
        }

        findByName("jvmMain")?.apply {
            dependsOn(codecMain)

            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(libs.logback.classic)
            }
        }

        if (findByName("mingwX64Main") != null) {
            val mingwMain = maybeCreate("mingwMain")
            nativeMain?.let { mingwMain.dependsOn(it) }
            mingwMain.dependsOn(codecMain)
            mingwMain.dependencies {
                implementation(libs.ktor.client.winhttp)
            }
            findByName("mingwX64Main")?.dependsOn(mingwMain)
        }

        if (findByName("macosArm64Main") != null) {
            val appleMain = maybeCreate("appleMain")
            nativeMain?.let { appleMain.dependsOn(it) }
            appleMain.dependsOn(codecMain)
            appleMain.dependencies {
                implementation(libs.ktor.client.darwin)
            }
            findByName("macosArm64Main")?.dependsOn(appleMain)
        }

        if (findByName("linuxX64Main") != null || findByName("linuxArm64Main") != null) {
            val linuxMain = maybeCreate("linuxMain")
            nativeMain?.let { linuxMain.dependsOn(it) }
            linuxMain.dependsOn(codecMain)
            linuxMain.dependencies {
                implementation(libs.ktor.client.curl)
            }
            findByName("linuxX64Main")?.dependsOn(linuxMain)
            findByName("linuxArm64Main")?.dependsOn(linuxMain)
        }

        if (findByName("androidNativeArm64Main") != null) {
            val androidNativeMain = maybeCreate("androidNativeMain")
            nativeMain?.let { androidNativeMain.dependsOn(it) } ?: androidNativeMain.dependsOn(commonMain)
            androidNativeMain.dependencies {
                implementation(libs.ktor.client.cio)
                implementation(libs.acidify.codec.androidnativearm64)
            }
            findByName("androidNativeArm64Main")?.dependsOn(androidNativeMain)
        }
    }

    targets.withType<KotlinNativeTarget> {
        binaries {
            executable {
                entryPoint = "org.ntqqrev.yogurt.main"
            }
        }
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        if (name == "mingwX64") {
            binaries.all {
                linkerOpts(
                    "-Wl,-Bstatic",
                    "-lstdc++",
                    "-lgcc",
                    "-Wl,-Bdynamic",
                )
            }
        }
    }
}

val gitHashProvider: Provider<String> = providers.exec {
    commandLine("git", "rev-parse", "HEAD")
}.standardOutput.asText.map { it.trim() }

val coreLibGitHashProvider: Provider<String> = providers.exec {
    commandLine("git", "-C", project(":acidify-core").projectDir.absolutePath, "rev-parse", "HEAD")
}.standardOutput.asText.map { it.trim() }

val buildTimeProvider: Provider<String> = providers.provider {
    ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))
}

val runNumberProvider: Provider<String> = providers.provider {
    System.getenv("GITHUB_RUN_NUMBER") ?: "local"
}

buildkonfig {
    packageName = "org.ntqqrev.yogurt"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "name", "Yogurt")
        buildConfigField(FieldSpec.Type.STRING, "version", "${project.version}-dev.${runNumberProvider.get()}")
        buildConfigField(
            FieldSpec.Type.STRING,
            "coreVersion",
            project(":acidify-core").let {
                "${it.name} ${it.version}+${coreLibGitHashProvider.get().substring(0, 7)}"
            }
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "milkyVersion",
            libs.milky.types.get().let {
                "${it.module.name} ${it.version}"
            }
        )
        buildConfigField(FieldSpec.Type.STRING, "commitHash", gitHashProvider.get())
        buildConfigField(FieldSpec.Type.STRING, "buildTime", buildTimeProvider.get())
    }
}
