import java.net.URI

plugins {
    alias(libs.plugins.android.application) //  đánh dấu module này là app, dùng plugin android application để tạo app
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

fun normalizeApiBaseUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return ""
    return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
}

fun validateApiBaseUrl(rawUrl: String, propertyName: String): URI {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) {
        throw GradleException("$propertyName rejected: API base URL must not be blank.")
    }
    val uri = try {
        val parsed = URI(trimmed)
        if (!parsed.isAbsolute) {
            throw GradleException("$propertyName rejected: API base URL must be absolute: '$trimmed'.")
        }
        parsed
    } catch (e: GradleException) {
        throw e
    } catch (e: Exception) {
        throw GradleException("$propertyName rejected: '$trimmed' is not a valid URI: ${e.message}", e)
    }

    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") {
        throw GradleException("$propertyName rejected: scheme must be http or https, but was '$scheme' in '$trimmed'.")
    }

    val host = uri.host ?: uri.authority?.substringAfter('@')?.substringBefore(':')
    if (host.isNullOrBlank()) {
        throw GradleException("$propertyName rejected: missing or blank host in '$trimmed'.")
    }

    return uri
}

fun isDemoApiUrl(url: String): Boolean {
    val trimmed = url.trim()
    val host = try {
        val uri = URI(trimmed)
        uri.host ?: uri.authority?.substringAfter('@')?.substringBefore(':')
    } catch (e: Exception) {
        null
    }
    return (host != null && host.contains("jsonplaceholder.typicode.com", ignoreCase = true)) ||
        trimmed.contains("jsonplaceholder.typicode.com", ignoreCase = true)
}

fun isExplicitDemoAllowed(project: Project): Boolean {
    val keys = listOf("ALLOW_DEMO_PROD_API", "allowDemoProd", "DEMO_MODE")
    return keys.any { key ->
        val prop = project.findProperty(key)?.toString()?.trim()
        val env = System.getenv(key)?.trim()
        (prop != null && (prop.isEmpty() || prop.equals("true", ignoreCase = true) || prop == "1")) ||
            (env != null && (env.isEmpty() || env.equals("true", ignoreCase = true) || env == "1"))
    }
}

fun escapeBuildConfigString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

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

    flavorDimensions += listOf("environment") // nhóm của project
    productFlavors { // 2 product flavor
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            val devApiBaseUrl = normalizeApiBaseUrl(
                (project.findProperty("DEV_API_BASE_URL") as? String)
                    ?: System.getenv("DEV_API_BASE_URL")
                    ?: "https://jsonplaceholder.typicode.com/"
            )
            val isDemo = isDemoApiUrl(devApiBaseUrl)
            buildConfigField("String", "ENVIRONMENT", "\"dev\"")
            buildConfigField("String", "API_BASE_URL", escapeBuildConfigString(devApiBaseUrl))
            buildConfigField("Boolean", "DEMO_MODE", "$isDemo")
        }
        create("prod") {
            dimension = "environment"
            val prodApiBaseUrl = normalizeApiBaseUrl(
                (project.findProperty("PROD_API_BASE_URL") as? String)
                    ?: System.getenv("PROD_API_BASE_URL")
                    ?: "https://jsonplaceholder.typicode.com/"
            )
            val isDemo = isDemoApiUrl(prodApiBaseUrl) || isExplicitDemoAllowed(project)
            buildConfigField("String", "ENVIRONMENT", "\"prod\"")
            buildConfigField("String", "API_BASE_URL", escapeBuildConfigString(prodApiBaseUrl))
            buildConfigField("Boolean", "DEMO_MODE", "$isDemo")
        }
    }

    // android sẽ kết hợp flavor + type (như vậy sẽ có 4 loại, nó sẽ tạo ra id app khác nhau)
    // variant: devĐebug, environment: dev, DEBUG: true -> id app: com.uzuu.letplay.dev.debug
    
    buildTypes { //2 loại build type
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
        // ngày trước viết buildFeatures ở đây
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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
        val rawProdApiBaseUrl = (project.findProperty("PROD_API_BASE_URL") as? String)
            ?: System.getenv("PROD_API_BASE_URL")
            ?: "https://jsonplaceholder.typicode.com/"

        val prodApiBaseUrl = normalizeApiBaseUrl(rawProdApiBaseUrl)
        val uri = validateApiBaseUrl(prodApiBaseUrl, "PROD_API_BASE_URL")

        val scheme = uri.scheme?.lowercase()
        val host = uri.host ?: uri.authority?.substringAfter('@')?.substringBefore(':') ?: ""
        val isLocal = host.equals("localhost", ignoreCase = true) || host == "127.0.0.1" || host == "10.0.2.2"
        val demoAuthorized = isExplicitDemoAllowed(project)

        if (scheme != "https" && !isLocal && !demoAuthorized) {
            throw GradleException(
                "prodRelease build rejected: PROD_API_BASE_URL must use HTTPS scheme in production release ('$prodApiBaseUrl'). " +
                "HTTP is only permitted for local testing or with explicit demo authorization."
            )
        }

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
