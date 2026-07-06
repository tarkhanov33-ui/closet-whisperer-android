import com.skydoves.whisperer.Configuration
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.parcelize)
  alias(libs.plugins.hilt.plugin)
}

android {
  namespace = "com.skydoves.whisperer"

  defaultConfig {
    minSdk = 21
    applicationId = "com.skydoves.whisperer"
    versionCode = Configuration.versionCode
    versionName = Configuration.versionName
    testInstrumentationRunner = "com.skydoves.whisperer.AppTestRunner"
  }

  buildFeatures {
    dataBinding = true
    buildConfig = true
  }

  hilt {
    enableAggregatingTask = true
  }

  kotlin {
    sourceSets.configureEach {
      kotlin.srcDir(layout.buildDirectory.files("generated/ksp/$name/kotlin/"))
    }
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }

  buildTypes {
    create("benchmark") {
      isDebuggable = true
      signingConfig = getByName("debug").signingConfig
      matchingFallbacks += listOf("release")
    }
  }
}

androidComponents {
  onVariants(selector().all()) { variant ->
    afterEvaluate {
      val variantName = variant.name.replaceFirstChar { it.uppercase() }
      val dataBindingTask =
        project.tasks.findByName("dataBindingGenBaseClasses$variantName")
      if (dataBindingTask != null) {
        project.tasks.getByName("ksp${variantName}Kotlin") {
          (this as? AbstractKotlinCompileTool<*>)?.setSource(dataBindingTask.outputs.files)
        }
      }
    }
  }
}

dependencies {
  // modules
  implementation(projects.coreData)

  // remote theming SDK (via JitPack)
  implementation("com.github.tarkhanov33-ui:RemoteUiSdk:1.0.0")



  // androidx
  implementation(libs.material)
  implementation(libs.androidx.fragment)
  implementation(libs.androidx.lifecycle)
  implementation(libs.androidx.startup)
  implementation(libs.androidx.palette)

  // data binding
  implementation(libs.bindables)

  // di
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  ksp(libs.kotlin.metadata.jvm)
  androidTestImplementation(libs.hilt.testing)
  kspAndroidTest(libs.hilt.compiler)
  kspAndroidTest(libs.kotlin.metadata.jvm)

  // coroutines
  implementation(libs.coroutines)

  // network
  implementation(libs.okhttp.interceptor)

  // whatIf
  implementation(libs.whatif)

  // image loading
  implementation(libs.glide)

  // bundler
  implementation(libs.bundler)

  // transformation animation
  implementation(libs.transformationLayout)

  // recyclerView
  implementation(libs.recyclerview)
  implementation(libs.baseAdapter)

  // custom views
  implementation(libs.rainbow)
  implementation(libs.androidRibbon)
  implementation(libs.progressView)

  // unit test
  testImplementation(libs.junit)
  testImplementation(libs.turbine)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.coroutines.test)
  androidTestImplementation(libs.truth)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso)
  androidTestImplementation(libs.android.test.runner)

  // Firebase Auth & Google Sign-In skeleton support
  implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
  implementation("com.google.android.gms:play-services-auth:21.0.0")
}
