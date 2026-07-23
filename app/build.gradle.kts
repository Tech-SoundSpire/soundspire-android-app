import java.util.Base64
import java.util.Properties

// Load release signing secrets from keystore.properties (gitignored) if present.
// Env vars (KEYSTORE_PATH / STORE_PASSWORD / KEY_PASSWORD) take precedence for CI.
val keystoreProps = Properties().apply {
  val f = rootProject.file("keystore.properties")
  if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(envKey: String, propKey: String): String? =
  System.getenv(envKey) ?: keystoreProps.getProperty(propKey)

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.soundspire.vsqtyz"
    minSdk = 24
    targetSdk = 36
    versionCode = 7
    versionName = "1.6"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Decode debug.keystore from debug.keystore.base64 if missing (e.g. when building locally)
  val keystoreFile = file("${rootDir}/debug.keystore")
  if (!keystoreFile.exists()) {
    val base64File = listOf(
      file("${rootDir}/debug.keystore.base64"),
      file("${projectDir}/../debug.keystore.base64"),
      file("${projectDir}/debug.keystore.base64")
    ).firstOrNull { it.exists() }
    
    if (base64File != null) {
      try {
        val base64Text = base64File.readText().replace("\\s".toRegex(), "")
        val decodedBytes = Base64.getDecoder().decode(base64Text)
        keystoreFile.writeBytes(decodedBytes)
        logger.lifecycle("Automatically decoded debug.keystore from base64 representation at configuration time.")
      } catch (e: Exception) {
        logger.error("Failed to decode debug.keystore: ${e.message}")
      }
    }
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH")
        ?: keystoreProps.getProperty("STORE_FILE")?.let { "${rootDir}/$it" }
        ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = signingValue("STORE_PASSWORD", "STORE_PASSWORD")
      keyAlias = signingValue("KEY_ALIAS", "KEY_ALIAS") ?: "upload"
      keyPassword = signingValue("KEY_PASSWORD", "KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  // implementation(libs.androidx.room.ktx)
  // implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.credentials)
  implementation(libs.credentials.play.services.auth)
  implementation(libs.googleid)
  implementation(libs.supabase.postgrest)
  implementation(libs.supabase.realtime)
  implementation(libs.ktor.client.okhttp)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  // "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

// Robust execution-phase task to ensure debug.keystore is decoded before signing/compilation
val decodeDebugKeystore = tasks.register("decodeDebugKeystore") {
  val keystoreFile = file("${rootDir}/debug.keystore")
  val base64File = sequenceOf(
    file("${rootDir}/debug.keystore.base64"),
    file("${projectDir}/../debug.keystore.base64"),
    file("${projectDir}/debug.keystore.base64")
  ).firstOrNull { it.exists() }

  if (base64File != null) {
    inputs.file(base64File)
  }
  outputs.file(keystoreFile)

  doLast {
    if (!keystoreFile.exists()) {
      if (base64File != null && base64File.exists()) {
        try {
          val base64Text = base64File.readText().replace("\\s".toRegex(), "")
          val decodedBytes = Base64.getDecoder().decode(base64Text)
          keystoreFile.writeBytes(decodedBytes)
          logger.lifecycle("Automatically decoded debug.keystore from base64 representation at execution time.")
        } catch (e: Exception) {
          throw GradleException("Failed to decode debug.keystore at execution time: ${e.message}", e)
        }
      } else {
        logger.warn("debug.keystore.base64 not found at any expected location!")
      }
    }
  }
}

// Hook decoding task into the Android build lifecycle and any signing/validation tasks
tasks.named("preBuild") {
  dependsOn(decodeDebugKeystore)
}

tasks.matching {
  it.name.contains("Signing", ignoreCase = true) ||
  it.name.contains("package", ignoreCase = true)
}.configureEach {
  dependsOn(decodeDebugKeystore)
}

// Copy the signed release APK to a versioned, shareable name in a separate dist/ dir
// (kept out of outputs/apk/release to avoid clashing with Android's own listing tasks).
val versionedReleaseApk = tasks.register<Copy>("versionedReleaseApk") {
  val versionName = android.defaultConfig.versionName ?: "1.0"
  from("${layout.buildDirectory.get()}/outputs/apk/release/app-release.apk")
  into("${layout.buildDirectory.get()}/dist")
  rename { "soundspire-v$versionName.apk" }
  mustRunAfter("assembleRelease")
  doLast { logger.lifecycle("Shareable APK: app/build/dist/soundspire-v$versionName.apk") }
}
tasks.matching { it.name == "assembleRelease" }.configureEach {
  finalizedBy(versionedReleaseApk)
}
