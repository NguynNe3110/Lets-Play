plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

fun isDemoApiUrl(url: String): Boolean =
    url.contains("jsonplaceholder.typicode.com", ignoreCase = true)

fun isExplicitDemoAllowed(project: Project): Boolean {
    val keys = listOf("ALLOW_DEMO_PROD_API", "allowDemoProd", "DEMO_MODE")
    return keys.any { key ->
        val prop = project.findProperty(key)?.toString()?.trim()
        val env = System.getenv(key)?.trim()
        (prop != null && (prop.isEmpty() || prop.equals("true", ignoreCase = true) || prop == "1")) ||
            (env != null && (env.isEmpty() || env.equals("true", ignoreCase = true) || env == "1"))
    }
}

android {
    namespace = "com.uzuu.letplay"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.uzuu.letplay"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions += listOf("environment")
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            val devApiBaseUrl = (project.findProperty("DEV_API_BASE_URL") as? String)
                ?: System.getenv("DEV_API_BASE_URL")
                ?: "https://jsonplaceholder.typicode.com/"
            val isDemo = isDemoApiUrl(devApiBaseUrl)
            buildConfigField("String", "ENVIRONMENT", "\"dev\"")
            buildConfigField("String", "API_BASE_URL", "\"$devApiBaseUrl\"")
            buildConfigField("Boolean", "DEMO_MODE", "$isDemo")
        }
        create("prod") {
            dimension = "environment"
            val prodApiBaseUrl = (project.findProperty("PROD_API_BASE_URL") as? String)
                ?: System.getenv("PROD_API_BASE_URL")
                ?: "https://jsonplaceholder.typicode.com/"
            val isDemo = isDemoApiUrl(prodApiBaseUrl) || isExplicitDemoAllowed(project)
            buildConfigField("String", "ENVIRONMENT", "\"prod\"")
            buildConfigField("String", "API_BASE_URL", "\"$prodApiBaseUrl\"")
            buildConfigField("Boolean", "DEMO_MODE", "$isDemo")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":resources"))
    implementation(project(":shared"))
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Hilt is the application composition root. Pure JVM modules remain DI-framework agnostic.
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}

kapt {
    correctErrorTypes = true
}

val checkProdReleaseConfig by tasks.registering {
    group = "verification"
    description = "Validates prodRelease configuration ensuring production release does not point to placeholder/demo API without explicit authorization."
    doLast {
        val prodApiBaseUrl = (project.findProperty("PROD_API_BASE_URL") as? String)
            ?: System.getenv("PROD_API_BASE_URL")
            ?: "https://jsonplaceholder.typicode.com/"

        val demoAuthorized = isExplicitDemoAllowed(project)
        val isDemo = isDemoApiUrl(prodApiBaseUrl)

        if (isDemo && !demoAuthorized) {
            throw GradleException(
                "prodRelease build rejected: PROD_API_BASE_URL is pointing to placeholder/demo URL ('$prodApiBaseUrl') " +
                "without explicit demo authorization. Production releases must specify a real production endpoint via " +
                "PROD_API_BASE_URL or explicitly authorize demo mode with -PallowDemoProd=true, -PALLOW_DEMO_PROD_API=true, or -PDEMO_MODE=true."
            )
        }
    }
}

tasks.matching {
    it.name == "assembleProdRelease" || it.name == "bundleProdRelease"
}.configureEach {
    dependsOn(checkProdReleaseConfig)
}
