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
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":acidify-core"))
                implementation(project(":acidify-milky"))
                implementation(project(":yogurt-fs"))
                implementation(libs.kotlinx.datetime)
                implementation(libs.bundles.ktor.client)
                implementation(libs.bundles.ktor.server)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.milky.types)
                implementation(libs.qr.matrix)
            }
        }

        val codecMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.acidify.codec)
            }
        }

        val jvmMain by getting {
            dependsOn(codecMain)
            dependencies {
                implementation(libs.ktor.client.java)
                implementation(libs.logback.classic)
            }
        }

        findByName("mingwMain")?.apply {
            dependsOn(codecMain)
            dependencies {
                implementation(libs.ktor.client.winhttp)
            }
        }
        findByName("appleMain")?.apply {
            dependsOn(codecMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        findByName("linuxMain")?.apply {
            dependsOn(codecMain)
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }
        val nativeMain = findByName("nativeMain")
        val androidNativeArm64Main = findByName("androidNativeArm64Main")
        if (nativeMain != null && androidNativeArm64Main != null) {
            androidNativeArm64Main.dependsOn(nativeMain)
        }
    }

    targets.withType<KotlinNativeTarget> {
        binaries {
            executable {
                entryPoint = "org.ntqqrev.yogurt.main"
            }
        }
    }

    targets.matching { it.name == "mingwX64" }.configureEach {
        (this as KotlinNativeTarget).binaries.all {
            linkerOpts(
                "-Wl,-Bstatic",
                "-lstdc++",
                "-lgcc",
                "-Wl,-Bdynamic",
            )
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
