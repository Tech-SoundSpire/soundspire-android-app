plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

// Generate SharedConfig.kt from the repo-root .env at build time, so both Android
// and iOS read the same config from one source. Values land only in the compiled
// output, never in logs. Mirrors the app's Secrets plugin, but cross-platform.
// Captures only File values so it is configuration-cache compatible.
val generateSharedConfig by tasks.registering {
    val envFile = rootProject.file(".env")
    val outDir = layout.buildDirectory.dir("generated/sharedconfig").get().asFile
    inputs.file(envFile).withPropertyName("env").optional(true)
    outputs.dir(outDir)
    doLast {
        val props = mutableMapOf<String, String>()
        if (envFile.exists()) {
            envFile.readLines().forEach { line ->
                val t = line.trim()
                if (t.isNotEmpty() && !t.startsWith("#") && t.contains("=")) {
                    props[t.substringBefore("=").trim()] =
                        t.substringAfter("=").trim().trim('"', '\'')
                }
            }
        }
        val api = (props["SOUNDSPIRE_API_BASE_URL"] ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
        val url = (props["SUPABASE_URL"] ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
        val key = (props["SUPABASE_ANON_KEY"] ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
        val gid = (props["GOOGLE_OAUTH_CLIENT_ID"] ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
        val giosid = (props["GOOGLE_IOS_CLIENT_ID"] ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
        val pkgDir = outDir.resolve("com/example/shared").apply { mkdirs() }
        pkgDir.resolve("SharedConfig.kt").writeText(
            """
            package com.example.shared
            // GENERATED from .env by the generateSharedConfig task. Do not edit.
            object SharedConfig {
                const val API_BASE_URL = "$api"
                const val SUPABASE_URL = "$url"
                const val SUPABASE_ANON_KEY = "$key"
                const val GOOGLE_OAUTH_CLIENT_ID = "$gid"
                const val GOOGLE_IOS_CLIENT_ID = "$giosid"
            }
            """.trimIndent()
        )
    }
}

kotlin {
    // Android target via AGP's KMP library plugin (required since AGP 9.0).
    android {
        namespace = "com.example.shared"
        compileSdk = 36
        minSdk = 24
    }
    // Device + simulator. Each emits a "Shared" framework for the iOS app.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateSharedConfig)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.realtime)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
