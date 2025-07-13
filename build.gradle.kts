import kotlin.io.path.copyTo
import kotlin.io.path.moveTo

plugins {
    id("com.android.application")
}

android {
    namespace = "top.sankokomi.wirebare.zygisk"
    compileSdk = libs.versions.targetSdk.get().toInt()
    ndkVersion = libs.versions.ndk.get()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
        ndk {
            abiFilters += listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a", "riscv64")
        }
    }
    externalNativeBuild {
        ndkBuild {
            path("jni/Android.mk")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.register<Task>("makeZygiskModule") {
    group = "zygisk"
    dependsOn("assembleRelease")
    doFirst {
        val zygiskBuildDir = layout.buildDirectory.dir("zygisk").get()
        zygiskBuildDir.asFile.deleteRecursively()
        val magiskDir = layout.projectDirectory.dir("magisk")
        // copy magisk module to build zygisk module
        magiskDir.asFileTree.visit {
            copyTo(zygiskBuildDir.file(path).asFile)
        }
        zygiskBuildDir.file("module.prop").asFile.let { moduleProp ->
            // write the version information into module.prop
            val prop = moduleProp.readLines().map {
                return@map when (it) {
                    "version=versionName" -> "version=${libs.versions.versionName.get()}"
                    "versionCode=versionCode" -> "versionCode=${libs.versions.versionCode.get()}"
                    else -> it
                }
            }
            moduleProp.writeText(prop.joinToString(separator = System.lineSeparator()))
        }
    }
    doLast {
        // copy .so into zygisk module
        val zygiskBuildDir = layout.buildDirectory.dir("zygisk").get()
        zygiskBuildDir.dir("zygisk").asFile.mkdirs()
        layout.buildDirectory.dir(
            "intermediates/stripped_native_libs/release/stripReleaseDebugSymbols/out/lib"
        ).get().asFileTree.visit {
            if (!isDirectory) return@visit
            File(file, "libwb.so").toPath().copyTo(
                zygiskBuildDir.file("zygisk/$path.so").asFile.toPath()
            )
        }
    }
}

tasks.register<Zip>("zipZygiskModule") {
    group = "zygisk"
    dependsOn("makeZygiskModule")
    // zip zygisk module
    val zygiskBuildDir = layout.buildDirectory.dir("zygisk").get()
    archiveBaseName.set("wirebare_certficate_installer")
    archiveVersion.set(android.defaultConfig.versionName)
    archiveExtension.set("zip")
    destinationDirectory.set(File(rootDir, "certificate").normalize())
    setMetadataCharset("UTF-8")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    from(zygiskBuildDir)
}
