import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.recetas.app"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.recetas.app"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0.0"
  }

  // Firma release desde android/keystore.properties (gitignoreado, lo crea release.ps1).
  // Sin ese archivo el release sale sin firmar.
  val ksProps = Properties()
  val ksFile = rootProject.file("keystore.properties")
  if (ksFile.exists()) ksFile.inputStream().use { ksProps.load(it) }

  signingConfigs {
    create("release") {
      if (ksProps.containsKey("storeFile")) {
        storeFile = rootProject.file(ksProps["storeFile"] as String)
        storePassword = ksProps["storePassword"] as String
        keyAlias = ksProps["keyAlias"] as String
        keyPassword = ksProps["keyPassword"] as String
      }
    }
  }

  buildTypes {
    release {
      signingConfig = signingConfigs.getByName("release")
      isMinifyEnabled = false
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
    }
  }
  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  implementation(libs.kotlinx.serialization.json)
  ksp(libs.androidx.room.compiler)
  testImplementation(libs.junit)
  testImplementation("org.xerial:sqlite-jdbc:3.47.2.0")
}
