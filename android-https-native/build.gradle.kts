import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.net.URL
import java.security.MessageDigest

plugins {
    kotlin("multiplatform")
}

version = "0.1.0"

val mbedTlsVersion = "3.6.5"
val caBundleDate = "2026-03-19"
val mbedTlsUrl = "https://github.com/Mbed-TLS/mbedtls/releases/download/mbedtls-$mbedTlsVersion/mbedtls-$mbedTlsVersion.tar.bz2"
val mbedTlsSha256 = "4a11f1777bb95bf4ad96721cac945a26e04bf19f57d905f241fe77ebeddf46d8"
val caBundleUrl = "https://curl.se/ca/cacert-$caBundleDate.pem"
val caBundleSha256 = "b6e66569cc3d438dd5abe514d0df50005d570bfc96c14dca8f768d020cb96171"

val userHome = System.getProperty("user.home")
val hostTag = when {
    System.getProperty("os.name").startsWith("Windows", ignoreCase = true) -> "windows"
    System.getProperty("os.name").startsWith("Mac", ignoreCase = true) -> "macos"
    else -> "linux"
}
val isWindowsHost = hostTag == "windows"
val konanDependenciesDir = file("$userHome/.konan/dependencies")
val toolchainRootDir = konanDependenciesDir.listFiles()
    ?.filter { it.isDirectory }
    ?.sortedByDescending { it.name }
    ?.firstOrNull { it.name.startsWith("target-toolchain-") && it.name.endsWith("-$hostTag-android_ndk") }
    ?: file("$userHome/.konan/dependencies/target-toolchain-2-$hostTag-android_ndk")
val toolchainBinDir = File(toolchainRootDir, "bin")
val clangExecutable = File(
    toolchainBinDir,
    if (isWindowsHost) "aarch64-linux-android21-clang.cmd" else "aarch64-linux-android21-clang"
)
val arExecutable = File(
    toolchainBinDir,
    if (isWindowsHost) "llvm-ar.exe" else "llvm-ar"
)

val downloadsDir = layout.buildDirectory.dir("downloads")
val thirdPartyRootDir = layout.buildDirectory.dir("third-party")
val mbedTlsArchive = downloadsDir.map { it.file("mbedtls-$mbedTlsVersion.tar.bz2") }
val mbedTlsSourceDir = thirdPartyRootDir.map { it.dir("mbedtls-$mbedTlsVersion") }
val caBundleFile = layout.buildDirectory.file("generated/ca/cacert.pem")
val generatedCinteropDir = layout.buildDirectory.dir("generated/cinterop")
val generatedDefFile = generatedCinteropDir.map { it.file("android_https_native.def") }
val nativeOutputRoot = layout.buildDirectory.dir("generated/androidHttpsNative")

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
    if (!destination.exists()) {
        URL(url).openStream().use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
    }
    verifySha256(destination, expectedSha256)
}

fun runCommand(command: List<String>, workingDirectory: File? = null) {
    val process = ProcessBuilder(command)
        .apply {
            if (workingDirectory != null) {
                directory(workingDirectory)
            }
            redirectErrorStream(true)
        }
        .start()
    val output = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()
    require(exitCode == 0) {
        buildString {
            append("Command failed with exit code ")
            append(exitCode)
            append(':')
            appendLine()
            append(command.joinToString(" "))
            if (output.isNotBlank()) {
                appendLine()
                append(output)
            }
        }
    }
}

val downloadMbedTls by tasks.registering {
    outputs.file(mbedTlsArchive)
    doLast {
        downloadVerified(mbedTlsUrl, mbedTlsArchive.get().asFile, mbedTlsSha256)
    }
}

val extractMbedTls by tasks.registering {
    dependsOn(downloadMbedTls)
    inputs.file(mbedTlsArchive)
    outputs.dir(mbedTlsSourceDir)
    doLast {
        delete(mbedTlsSourceDir.get().asFile)
        copy {
            from(tarTree(resources.bzip2(mbedTlsArchive.get().asFile)))
            into(thirdPartyRootDir)
        }
        require(mbedTlsSourceDir.get().file("include/mbedtls/ssl.h").asFile.exists()) {
            "Extracted mbedTLS tree is incomplete: ${mbedTlsSourceDir.get().asFile}"
        }
    }
}

val prepareAndroidNativeCaBundle by tasks.registering {
    outputs.file(caBundleFile)
    doLast {
        downloadVerified(caBundleUrl, caBundleFile.get().asFile, caBundleSha256)
    }
}

val buildAndroidHttpsNative by tasks.registering {
    dependsOn(extractMbedTls)
    inputs.dir(mbedTlsSourceDir)
    inputs.file(layout.projectDirectory.file("src/androidNativeArm64Main/c/android_https_native.c"))
    inputs.file(layout.projectDirectory.file("src/androidNativeArm64Main/c/include/android_https_native.h"))
    outputs.file(generatedDefFile)
    outputs.file(nativeOutputRoot.map { it.file("lib/libandroid_https_native.a") })
    doLast {
        require(clangExecutable.exists()) { "Android NDK clang not found: $clangExecutable" }
        require(arExecutable.exists()) { "Android NDK llvm-ar not found: $arExecutable" }

        val mbedTlsDir = mbedTlsSourceDir.get().asFile
        val outputRoot = nativeOutputRoot.get().asFile.apply { mkdirs() }
        val objectDir = File(outputRoot, "obj").apply {
            deleteRecursively()
            mkdirs()
        }
        val libDir = File(outputRoot, "lib").apply { mkdirs() }
        val includeDir = File(outputRoot, "include").apply {
            deleteRecursively()
            mkdirs()
        }
        copy {
            from(layout.projectDirectory.file("src/androidNativeArm64Main/c/include/android_https_native.h"))
            into(includeDir)
        }
        val generatedDef = generatedDefFile.get().asFile.apply {
            parentFile.mkdirs()
        }
        generatedDef.writeText(
            (
                """
                headers = ${File(includeDir, "android_https_native.h").invariantSeparatorsPath}
                package = org.ntqqrev.androidhttps.native
                compilerOpts = -I${includeDir.invariantSeparatorsPath}
                staticLibraries = libandroid_https_native.a
                libraryPaths = ${libDir.invariantSeparatorsPath}
                """.trimIndent() + System.lineSeparator()
            )
        )

        val allSources = fileTree(File(mbedTlsDir, "library")) {
            include("*.c")
        }.files.sortedBy { it.name } + layout.projectDirectory.file("src/androidNativeArm64Main/c/android_https_native.c").asFile

        val objectFiles = mutableListOf<File>()
        allSources.forEachIndexed { index, sourceFile ->
            val objectFile = File(objectDir, "${index}_${sourceFile.nameWithoutExtension}.o")
            runCommand(
                listOf(
                    clangExecutable.absolutePath,
                    "-c",
                    sourceFile.absolutePath,
                    "-o",
                    objectFile.absolutePath,
                    "-std=c11",
                    "-O2",
                    "-fPIC",
                    "-D_POSIX_C_SOURCE=200809L",
                    "-I${File(mbedTlsDir, "include").absolutePath}",
                    "-I${File(mbedTlsDir, "library").absolutePath}",
                    "-I${layout.projectDirectory.dir("src/androidNativeArm64Main/c/include").asFile.absolutePath}",
                )
            )
            objectFiles += objectFile
        }

        val staticLibrary = File(libDir, "libandroid_https_native.a")
        runCommand(listOf(arExecutable.absolutePath, "rcs", staticLibrary.absolutePath) + objectFiles.map { it.absolutePath })
    }
}

kotlin {
    androidNativeArm64()

    sourceSets {
        findByName("androidNativeArm64Main")?.dependencies {
            implementation(libs.ktor.client.core)
        }
    }

    targets.withType<KotlinNativeTarget>().matching { it.name == "androidNativeArm64" }.configureEach {
        compilations.getByName("main") {
            val androidHttpsNative by cinterops.creating {
                definitionFile.set(generatedDefFile)
                tasks.named(interopProcessingTaskName).configure {
                    dependsOn(buildAndroidHttpsNative)
                }
            }
        }
    }
}

afterEvaluate {
    tasks.findByName("downloadKotlinNativeDistribution")?.let { downloadTask ->
        buildAndroidHttpsNative.configure {
            dependsOn(downloadTask)
        }
    }
}
