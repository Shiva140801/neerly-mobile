import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Firebase (push + phone auth) needs app/google-services.json, which is gitignored.
// Apply the plugin only when the file is present so CI and fresh clones still build.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.neerly.mobile"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.neerly.mobile"
        minSdk = 26   // java.time everywhere; Android 7 (24–25) would crash without desugaring
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Google Maps key stays out of VCS: add MAPS_API_KEY=... to local.properties.
        // An absent key still builds — the map just renders blank tiles.
        val props = Properties()
        rootProject.file("local.properties").takeIf { it.exists() }
            ?.inputStream()?.use { props.load(it) }
        val mapsKey: String = props.getProperty("MAPS_API_KEY") ?: ""
        manifestPlaceholders["MAPS_API_KEY"] = mapsKey
        // Screens hide the map rather than show blank tiles when no key is set.
        buildConfigField("Boolean", "MAPS_ENABLED", if (mapsKey.isBlank()) "false" else "true")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // The API address per build. Debug talks to the emulator's host (override with
    // API_BASE_URL=... in local.properties to reach another port or a LAN backend);
    // release needs a real server: -PreleaseApiBaseUrl=https://… or RELEASE_API_BASE_URL in
    // local.properties. A release used to ship pointing at 10.0.2.2.
    val localProps = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    val debugApi = localProps.getProperty("API_BASE_URL") ?: "http://10.0.2.2:8080/"
    val releaseApi = (project.findProperty("releaseApiBaseUrl") as String?)
        ?: localProps.getProperty("RELEASE_API_BASE_URL") ?: "https://api.boondh.in/"

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"$debugApi\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApi\"")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    testOptions {
        // MockK inline instrumentation is heap-hungry; the 512m default OOMs
        // when the whole suite runs in one JVM.
        unitTests.all {
            it.maxHeapSize = "2g"
            // MockK's Byte Buddy cannot instrument JDK 25 classes (the JDK
            // Gradle's auto-provisioned daemon runs on); every inline transform
            // fails and the retry churn OOMs the worker. Pin test workers to a
            // supported LTS.
            it.javaLauncher.set(
                project.extensions.getByType(org.gradle.jvm.toolchain.JavaToolchainService::class.java)
                    .launcherFor { languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(21)) }
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.lifecycle.vm)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)

    implementation(libs.coroutines)
    implementation(libs.coil.compose)
    implementation(libs.timber)
    implementation(libs.datastore.prefs)
    implementation(libs.security.crypto)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)

    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Razorpay Checkout for in-app UPI / card payments
    implementation(libs.razorpay.checkout)

    // --- Testing ---
    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    debugImplementation(libs.compose.ui.test.manifest)
}
