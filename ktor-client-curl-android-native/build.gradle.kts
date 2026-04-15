@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.net.URL
import java.security.MessageDigest

plugins {
    kotlin("multiplatform")
}

val caBundleUrl = "https://curl.se/ca/cacert.pem"
val caBundleSha256Url = "https://curl.se/ca/cacert.pem.sha256"
val caBundleFile = layout.buildDirectory.file("generated/ca/cacert.pem")
val generatedCaSourceDir = layout.buildDirectory.dir("generated/embeddedCa/src/androidNativeArm64Main/kotlin")
val generatedCaSourceFile = generatedCaSourceDir.map { it.file("io/ktor/client/engine/curl/internal/EmbeddedAndroidNativeCa.kt") }

fun verifySha256(file: File, expectedSha256: String) {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(file.readBytes())
        .joinToString("") { "%02x".format(it) }
    require(digest.equals(expectedSha256, ignoreCase = true)) {
        "SHA-256 mismatch for ${file.name}: expected $expectedSha256 but got $digest"
    }
}

fun downloadVerified(url: String, destination: File, expectedSha256: String) {
    destination.parentFile.mkdirs()
    if (destination.exists()) {
        destination.delete()
    }
    URL(url).openStream().use { input ->
        destination.outputStream().use { output -> input.copyTo(output) }
    }
    verifySha256(destination, expectedSha256)
}

fun downloadLatestCaBundle(destination: File) {
    val expectedSha256 = URL(caBundleSha256Url)
        .readText()
        .lineSequence()
        .firstOrNull()
        ?.trim()
        ?.substringBefore(' ')
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: error("Unable to resolve SHA-256 for latest CA bundle from $caBundleSha256Url")
    downloadVerified(caBundleUrl, destination, expectedSha256)
}

fun generateEmbeddedCaKotlinSource(caBundle: File, destination: File) {
    destination.parentFile.mkdirs()
    val pemText = caBundle.readText()
    destination.writeText(
        buildString {
            appendLine("package io.ktor.client.engine.curl.internal")
            appendLine()
            appendLine("internal val EMBEDDED_ANDROID_NATIVE_CA_PEM: String = \"\"\"")
            append(pemText)
            if (!pemText.endsWith("\n")) {
                appendLine()
            }
            appendLine("\"\"\".trimIndent()")
        }
    )
}

val prepareAndroidNativeCurlCaBundle by tasks.registering {
    outputs.file(caBundleFile)
    outputs.upToDateWhen { false }
    doLast {
        downloadLatestCaBundle(caBundleFile.get().asFile)
    }
}

val generateEmbeddedAndroidNativeCurlCaSource by tasks.registering {
    dependsOn(prepareAndroidNativeCurlCaBundle)
    inputs.file(caBundleFile)
    outputs.file(generatedCaSourceFile)
    doLast {
        generateEmbeddedCaKotlinSource(caBundleFile.get().asFile, generatedCaSourceFile.get().asFile)
    }
}

kotlin {
    androidNativeArm64()

    sourceSets {
        androidNativeMain.dependencies {
            implementation(libs.ktor.client.core)
        }
        findByName("androidNativeArm64Main")?.kotlin?.srcDir(generatedCaSourceDir)
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

afterEvaluate {
    tasks.findByName("compileKotlinAndroidNativeArm64")?.dependsOn(generateEmbeddedAndroidNativeCurlCaSource)
}
